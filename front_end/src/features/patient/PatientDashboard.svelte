<script lang="ts">
  import { onMount } from 'svelte';
  import Alert from '../../design-system/components/Alert.svelte';
  import Button from '../../design-system/components/Button.svelte';
  import Card from '../../design-system/components/Card.svelte';
  import StatusBadge from '../../design-system/components/StatusBadge.svelte';
  import Tooltip from '../../design-system/components/Tooltip.svelte';
  import { currentUser, patientApi, type ApiAppointment, type ApiVaccine } from '../../lib/api';
  let { onNavigate }: { onNavigate: (page: string) => void } = $props();
  type DashboardAppointment = {
    id: number; vaccine: string; date: Date; dose: string; location: string;
    status: 'confirmed' | 'pending' | 'completed' | 'cancelled';
  };
  const user = currentUser();
  let appointments = $state<DashboardAppointment[]>([]);
  let applicationCount = $state(0);
  let dependentCount = $state(0);
  let loading = $state(true);
  let error = $state('');
  let nextAppointment = $derived(appointments.find((item) => item.status !== 'cancelled'));

  function mapAppointment(item: ApiAppointment, vaccines: ApiVaccine[]): DashboardAppointment {
    const statuses: Record<string, DashboardAppointment['status']> = {
      CONFIRMADO: 'confirmed', PENDENTE: 'pending', CONCLUIDO: 'completed', CANCELADO: 'cancelled',
    };
    return {
      id: item.id,
      vaccine: vaccines.find((vaccine) => vaccine.id === item.vacinaId)?.nome ?? `Vacina #${item.vacinaId}`,
      date: new Date(item.dataAgendamento), dose: item.dosePrevista,
      location: [item.unidade, item.sala].filter(Boolean).join(' · '),
      status: statuses[item.status] ?? 'pending',
    };
  }

  onMount(async () => {
    if (!user) { loading = false; return; }
    try {
      const [apiAppointments, vaccines, applications, dependents] = await Promise.all([
        patientApi.appointments(user.id), patientApi.vaccines(),
        patientApi.applications(user.id), patientApi.dependents(),
      ]);
      appointments = apiAppointments
        .map((item) => mapAppointment(item, vaccines))
        .filter((item) => item.date >= new Date() && item.status !== 'cancelled')
        .sort((a, b) => a.date.getTime() - b.date.getTime());
      applicationCount = applications.length;
      dependentCount = dependents.length;
    } catch (exception) {
      error = exception instanceof Error ? exception.message : 'Não foi possível carregar o painel.';
    } finally { loading = false; }
  });
</script>

<div class="page">
  <header class="page-header">
    <div>
      <p class="eyebrow">Portal do paciente</p>
      <div class="title-row">
        <h1>Olá, {user?.nome.split(' ')[0] ?? 'paciente'}</h1>
        <Tooltip text="Acompanhe seus agendamentos e mantenha sua vacinação em dia." />
      </div>
    </div>
    <Button onclick={() => onNavigate('booking')}>Agendar vacina</Button>
  </header>
  {#if error}<Alert tone="danger">{error}</Alert>{/if}
  {#if loading}<p class="loading">Carregando seu painel...</p>{/if}

  <section class="hero-grid" aria-label="Destaques">
    <article class="hero primary">
      <span class="hero-icon">✦</span>
      <div>
        <p>Próximo agendamento</p>
        <h2>{nextAppointment?.vaccine ?? 'Nenhum horário reservado'}</h2>
        <strong>{nextAppointment ? `${nextAppointment.date.toLocaleDateString('pt-BR')} às ${nextAppointment.date.toLocaleTimeString('pt-BR', { hour:'2-digit', minute:'2-digit' })}` : 'Agende quando precisar'}</strong>
      </div>
      <button onclick={() => onNavigate('appointments')}>Ver detalhes →</button>
    </article>
    <article class="hero accent">
      <span class="hero-icon">✓</span>
      <div>
        <p>Sua carteira, sempre com você</p>
        <h2>Histórico de vacinação</h2>
        <strong>{applicationCount} aplicações registradas</strong>
      </div>
      <button onclick={() => onNavigate('history')}>Acessar carteira →</button>
    </article>
    <article class="hero navy">
      <span class="hero-icon">◎</span>
      <div>
        <p>Proteção para sua família</p>
        <h2>Gerencie dependentes</h2>
        <strong>{dependentCount} {dependentCount === 1 ? 'dependente vinculado' : 'dependentes vinculados'}</strong>
      </div>
      <button onclick={() => onNavigate('family')}>Minha família →</button>
    </article>
  </section>

  <section class="section">
    <div class="section-heading">
      <div class="section-title">
        <h2>Próximos agendamentos</h2>
        <Tooltip text="Seus atendimentos confirmados e pendentes." />
      </div>
      <button class="text-link" onclick={() => onNavigate('appointments')}>Ver todos</button>
    </div>
    <div class="appointment-grid">
      {#each appointments.slice(0, 4) as appointment}
        <Card padding="sm">
          <div class="appointment-card">
            <div class="date-box">
              <strong>{appointment.date.getDate()}</strong><span>{appointment.date.toLocaleDateString('pt-BR', { month:'short' }).replace('.', '')}</span>
            </div>
            <div class="appointment-info">
              <div class="appointment-title">
                <h3>{appointment.vaccine}</h3>
                <StatusBadge status={appointment.status} />
              </div>
              <p>{appointment.dose} · {appointment.date.toLocaleTimeString('pt-BR', { hour:'2-digit', minute:'2-digit' })}</p>
              <small>{appointment.location}</small>
            </div>
          </div>
        </Card>
      {/each}
      {#if !loading && appointments.length === 0}<Card padding="sm"><p class="empty">Você não possui próximos agendamentos.</p></Card>{/if}
    </div>
  </section>

  <section class="section">
    <div class="section-heading">
      <div class="section-title">
        <h2>Acessos rápidos</h2>
        <Tooltip text="Encontre o que precisa sem complicação." />
      </div>
    </div>
    <div class="shortcut-grid">
      {#each [{ icon: '✚', label: 'Agendar vacina', page: 'booking' }, { icon: '▤', label: 'Carteira vacinal', page: 'history' }, { icon: '♧', label: 'Minha família', page: 'family' }, { icon: '◇', label: 'Meus convênios', page: 'insurance' }] as item}
        <button onclick={() => onNavigate(item.page)}><span>{item.icon}</span>{item.label}</button>
      {/each}
    </div>
  </section>
</div>

<style>
  .page {
    width: min(100%, var(--content-max));
    margin: 0 auto;
    padding: var(--space-8);
  }
  .loading, .empty { color: var(--text-secondary); font-size: var(--text-sm); }
  .loading { margin-top: var(--space-4); }
  .page-header,
  .section-heading {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: var(--space-6);
  }
  .eyebrow {
    margin-bottom: var(--space-2);
    color: var(--color-brand-500);
    font-size: var(--text-xs);
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }
  h1 {
    font-size: clamp(var(--text-2xl), 3vw, var(--text-3xl));
    line-height: var(--leading-tight);
    letter-spacing: -0.035em;
  }
  .title-row,
  .section-title {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }
  .hero-grid {
    display: grid;
    grid-template-columns: 1.15fr 1fr 1fr;
    gap: var(--space-4);
    margin-top: var(--space-8);
  }
  .hero {
    display: flex;
    min-height: 11.5rem;
    flex-direction: column;
    justify-content: space-between;
    overflow: hidden;
    border-radius: var(--radius-lg);
    padding: var(--space-5);
    box-shadow: var(--shadow-sm);
  }
  .hero.primary {
    background: var(--color-brand-500);
    color: white;
  }
  .hero.accent {
    background: var(--color-accent-400);
    color: #111827;
  }
  .hero.navy {
    background: var(--color-navy-900);
    color: white;
  }
  .hero-icon {
    display: grid;
    width: 2rem;
    height: 2rem;
    place-items: center;
    border: 1px solid currentColor;
    border-radius: var(--radius-md);
    font-size: 0.9rem;
    opacity: 0.9;
  }
  .hero p {
    margin-bottom: var(--space-1);
    font-size: var(--text-xs);
    opacity: 0.78;
  }
  .hero h2 {
    max-width: 15rem;
    font-size: var(--text-lg);
    line-height: 1.25;
  }
  .hero strong {
    display: block;
    margin-top: var(--space-2);
    font-size: var(--text-xs);
    line-height: 1.4;
    opacity: 0.82;
  }
  .hero button {
    align-self: flex-start;
    border: 0;
    border-bottom: 1px solid currentColor;
    background: transparent;
    padding: 0.25rem 0;
    color: inherit;
    font-size: var(--text-xs);
    font-weight: 750;
    cursor: pointer;
  }
  .section {
    margin-top: var(--space-8);
  }
  .section-heading {
    margin-bottom: var(--space-4);
  }
  .section-heading h2 {
    font-size: var(--text-xl);
    letter-spacing: -0.02em;
  }
  .text-link {
    border: 0;
    background: transparent;
    color: var(--color-brand-500);
    font-size: var(--text-sm);
    font-weight: 700;
    cursor: pointer;
  }
  .appointment-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-4);
  }
  .appointment-card {
    display: flex;
    align-items: center;
    gap: var(--space-5);
  }
  .date-box {
    display: grid;
    min-width: 3.35rem;
    min-height: 3.35rem;
    place-content: center;
    border-radius: var(--radius-md);
    background: var(--color-brand-50);
    color: var(--color-brand-600);
    text-align: center;
  }
  :global([data-theme='dark']) .date-box {
    background: rgb(17 124 111 / 0.2);
    color: #70c9bd;
  }
  .date-box strong {
    font-size: var(--text-xl);
    line-height: 1;
  }
  .date-box span {
    margin-top: 0.2rem;
    font-size: var(--text-xs);
    text-transform: uppercase;
  }
  .appointment-info {
    min-width: 0;
    flex: 1;
  }
  .appointment-title {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-3);
  }
  .appointment-title h3 {
    font-size: var(--text-md);
  }
  .appointment-info p {
    margin-top: var(--space-2);
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
  .appointment-info small {
    display: block;
    margin-top: var(--space-1);
    color: var(--text-tertiary);
  }
  .shortcut-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-4);
  }
  .shortcut-grid button {
    display: flex;
    min-height: 5.5rem;
    flex-direction: column;
    align-items: flex-start;
    justify-content: space-between;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: var(--space-4);
    color: var(--text-primary);
    font-size: var(--text-sm);
    font-weight: 700;
    cursor: pointer;
    box-shadow: var(--shadow-sm);
    transition:
      transform var(--transition-fast),
      border-color var(--transition-fast);
  }
  .shortcut-grid button:hover {
    transform: translateY(-2px);
    border-color: var(--color-brand-500);
  }
  .shortcut-grid span {
    color: var(--color-brand-500);
    font-size: 1.35rem;
  }
  @media (max-width: 1100px) {
    .hero-grid {
      grid-template-columns: repeat(2, 1fr);
    }
    .hero:last-child {
      grid-column: 1 / -1;
      min-height: 10rem;
    }
    .shortcut-grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }
  @media (max-width: 680px) {
    .page {
      padding: var(--space-5);
    }
    .page-header {
      align-items: flex-start;
      flex-direction: column;
    }
    .hero-grid,
    .appointment-grid {
      grid-template-columns: 1fr;
    }
    .hero:last-child {
      grid-column: auto;
    }
    .hero {
      min-height: 11rem;
    }
    .shortcut-grid {
      grid-template-columns: 1fr 1fr;
    }
    .appointment-title {
      flex-direction: column;
    }
    .section-heading {
      align-items: flex-start;
    }
  }
</style>
