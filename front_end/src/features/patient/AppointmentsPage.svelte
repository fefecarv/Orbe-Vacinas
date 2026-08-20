<script lang="ts">
  import { onMount } from 'svelte';
  import Button from '../../design-system/components/Button.svelte';
  import Card from '../../design-system/components/Card.svelte';
  import StatusBadge from '../../design-system/components/StatusBadge.svelte';
  import ConfirmDialog from '../../design-system/components/ConfirmDialog.svelte';
  import Toast from '../../design-system/components/Toast.svelte';
  import CollectionPanel from '../../design-system/components/CollectionPanel.svelte';
  import ViewModeToggle from '../../design-system/components/ViewModeToggle.svelte';
  import { currentUser, patientApi, type ApiAppointment, type ApiVaccine } from '../../lib/api';
  let { onSchedule }: { onSchedule: () => void } = $props();
  let selectedAppointment = $state('');
  let pendingAction = $state<'reschedule' | 'cancel' | 'details' | ''>('');
  let toast = $state('');
  type AppointmentView = {
    id: string;
    vaccine: string;
    manufacturer: string;
    date: string;
    time: string;
    location: string;
    dose: string;
    status: 'confirmed' | 'pending' | 'waiting' | 'in_service' | 'completed' | 'cancelled' | 'missed';
    manageable: boolean;
    cancellationReason?: string;
  };
  const user = currentUser();
  let people = $state([{ id: `u:${user?.id ?? ''}`, name: user?.nome ?? 'Titular' }]);
  let items = $state<AppointmentView[]>([]);
  let newDate = $state(new Date(Date.now() + 86_400_000).toISOString().slice(0, 10));
  let newTime = $state('10:00');
  let cancellationReason = $state('Imprevisto pessoal');
  let selectedPatient = $state(`u:${user?.id ?? ''}`);
  let loading = $state(true);
  let loadError = $state('');
  let viewMode = $state<'grid' | 'list'>((localStorage.getItem('orbe-view-appointments') as 'grid' | 'list') ?? 'grid');
  let selectedRecord = $derived(items.find((item) => item.id === selectedAppointment));
  $effect(() => localStorage.setItem('orbe-view-appointments', viewMode));
  function mapAppointment(item: ApiAppointment, vaccines: ApiVaccine[]): AppointmentView {
    const vaccine = vaccines.find((candidate) => candidate.id === item.vacinaId);
    const date = new Date(item.dataAgendamento);
    const expired = date.getTime() < Date.now() && ['PENDENTE', 'CONFIRMADO'].includes(item.status);
    const statuses: Record<string, AppointmentView['status']> = {
      CONFIRMADO: 'confirmed',
      PENDENTE: 'pending',
      ESPERA: 'waiting',
      EM_ATENDIMENTO: 'in_service',
      CONCLUIDO: 'completed',
      CANCELADO: 'cancelled',
      FALTOU: 'missed',
    };
    return {
      id: String(item.id),
      vaccine: vaccine?.nome ?? `Vacina #${item.vacinaId}`,
      manufacturer: vaccine?.fabricante ?? 'Fabricante não informado',
      date: date.toLocaleDateString('pt-BR', { dateStyle: 'long' }),
      time: date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
      location: [item.unidade, item.sala].filter(Boolean).join(' · '),
      dose: item.dosePrevista,
      status: expired ? 'missed' : (statuses[item.status] ?? 'pending'),
      cancellationReason: item.motivoCancelamento ?? undefined,
      manageable: !expired && ['PENDENTE', 'CONFIRMADO'].includes(item.status),
    };
  }
  async function loadAppointments(silent = false) {
    const [kind, rawId] = selectedPatient.split(':');
    const patientId = Number(rawId);
    if (!patientId) return;
    if (!silent) loading = true;
    loadError = '';
    try {
      const [appointments, vaccines] = await Promise.all([
        kind === 'd' ? patientApi.appointmentsForDependent(patientId) : patientApi.appointments(patientId),
        patientApi.vaccines(),
      ]);
      items = appointments.map((item) => mapAppointment(item, vaccines));
    } catch (exception) {
      loadError = exception instanceof Error ? exception.message : 'Não foi possível carregar os agendamentos.';
    } finally {
      if (!silent) loading = false;
    }
  }
  async function confirmAction() {
    if (pendingAction === 'details') {
      pendingAction = '';
      selectedAppointment = '';
      return;
    }
    try {
      if (pendingAction === 'cancel')
        await patientApi.cancelAppointment(Number(selectedAppointment), cancellationReason);
      else await patientApi.rescheduleAppointment(Number(selectedAppointment), `${newDate}T${newTime}:00`);
      await loadAppointments();
      toast =
        pendingAction === 'cancel' ? 'Agendamento cancelado com sucesso.' : 'Agendamento atualizado para a nova data.';
    } catch (exception) {
      toast = exception instanceof Error ? exception.message : 'Não foi possível atualizar o agendamento.';
    }
    pendingAction = '';
    selectedAppointment = '';
  }
  onMount(() => {
    void (async () => {
      try {
        const dependents = await patientApi.dependents();
        people = [people[0], ...dependents.map((item) => ({ id: `d:${item.id}`, name: item.nome }))];
      } catch (exception) {
        loadError = exception instanceof Error ? exception.message : 'Não foi possível carregar os dependentes.';
      }
      await loadAppointments();
    })();
    const refresh = window.setInterval(() => void loadAppointments(true), 15_000);
    const refreshOnFocus = () => void loadAppointments(true);
    window.addEventListener('focus', refreshOnFocus);
    return () => {
      window.clearInterval(refresh);
      window.removeEventListener('focus', refreshOnFocus);
    };
  });
</script>

<div class="page">
  <header>
    <div>
      <p class="eyebrow">Vacinação</p>
      <h1>Agendamentos</h1>
      <p>Acompanhe e gerencie as próximas vacinas de {user?.nome.split(' ')[0] ?? 'sua família'}.</p>
    </div>
    <Button onclick={onSchedule}>Agendar vacina</Button>
  </header>
  <label class="patient-pill"
    >Carteira de <select bind:value={selectedPatient} onchange={() => loadAppointments()}
      >{#each people as person}<option value={person.id}>{person.name}</option>{/each}</select
    ></label
  >
  <div class="collection">
    <CollectionPanel title="Agendamentos" description="Alterne entre grade e lista conforme sua preferência.">
      {#snippet actions()}<ViewModeToggle bind:value={viewMode} />{/snippet}
      {#if loadError}<p class="load-message error">{loadError}</p>{/if}
      {#if loading}<p class="load-message">Carregando agendamentos...</p>{/if}
      <div class="list {viewMode}">
        {#each items as appointment}<Card
            ><article>
              <div>
                <StatusBadge status={appointment.status} />
                <h3>{appointment.vaccine}</h3>
                <p>{appointment.dose} · {appointment.manufacturer}</p>
              </div>
              <dl>
                <div>
                  <dt>Data</dt>
                  <dd>{appointment.date}</dd>
                </div>
                <div>
                  <dt>Horário</dt>
                  <dd>{appointment.time}</dd>
                </div>
                <div>
                  <dt>Local</dt>
                  <dd>{appointment.location}</dd>
                </div>
              </dl>
              <div class="actions">
                <Button
                  variant="secondary"
                  size="sm"
                  onclick={() => {
                    selectedAppointment = appointment.id;
                    pendingAction = 'details';
                  }}>Ver detalhes</Button
                >{#if appointment.manageable}<Button
                    variant="ghost"
                    size="sm"
                    onclick={() => {
                      selectedAppointment = appointment.id;
                      pendingAction = 'reschedule';
                    }}>Reagendar</Button
                  ><button
                    class="cancel"
                    onclick={() => {
                      selectedAppointment = appointment.id;
                      pendingAction = 'cancel';
                    }}>Cancelar</button
                  >{/if}
              </div>
            </article></Card
          >{/each}{#if !loading && items.length === 0}<Card><p>Nenhum agendamento encontrado para esta pessoa.</p></Card
          >{/if}
      </div>
    </CollectionPanel>
  </div>
</div>
{#if pendingAction}<ConfirmDialog
    title={pendingAction === 'details'
      ? 'Detalhes do agendamento'
      : pendingAction === 'cancel'
        ? 'Cancelar agendamento?'
        : 'Escolha o novo horário'}
    description={pendingAction === 'details'
      ? 'Confira os dados completos do atendimento.'
      : pendingAction === 'cancel'
        ? 'O horário será liberado e o cancelamento permanecerá no histórico.'
        : 'A data anterior será preservada no histórico do agendamento.'}
    confirmLabel={pendingAction === 'details'
      ? 'Fechar'
      : pendingAction === 'cancel'
        ? 'Cancelar agendamento'
        : 'Confirmar reagendamento'}
    danger={pendingAction === 'cancel'}
    onConfirm={confirmAction}
    onCancel={() => {
      pendingAction = '';
      selectedAppointment = '';
    }}
    >{#snippet children()}{#if pendingAction === 'details' && selectedRecord}<dl>
          <div>
            <dt>Vacina</dt>
            <dd>{selectedRecord.vaccine}</dd>
          </div>
          <div>
            <dt>Dose</dt>
            <dd>{selectedRecord.dose}</dd>
          </div>
          <div>
            <dt>Data</dt>
            <dd>{selectedRecord.date} às {selectedRecord.time}</dd>
          </div>
          <div>
            <dt>Local</dt>
            <dd>{selectedRecord.location}</dd>
          </div>
          {#if selectedRecord.cancellationReason}<div>
              <dt>Motivo do cancelamento</dt>
              <dd>{selectedRecord.cancellationReason}</dd>
            </div>{/if}
        </dl>{:else if pendingAction === 'cancel'}<label class="dialog-field"
          >Motivo<select bind:value={cancellationReason}
            ><option>Imprevisto pessoal</option><option>Problema de saúde</option><option>Agendamento duplicado</option
            ><option>Outro motivo</option></select
          ></label
        >{:else}<div class="dialog-grid">
          <label class="dialog-field"
            >Nova data<input type="date" min={new Date().toISOString().slice(0, 10)} bind:value={newDate} /></label
          ><label class="dialog-field"
            >Novo horário<select bind:value={newTime}
              ><option>09:00</option><option>10:00</option><option>14:30</option></select
            ></label
          >
        </div>{/if}{/snippet}</ConfirmDialog
  >{/if}
{#if toast}<Toast message={toast} onClose={() => (toast = '')} />{/if}

<style>
  .page {
    width: min(100%, var(--content-max));
    margin: 0 auto;
    padding: var(--space-8);
  }
  header {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: var(--space-6);
  }
  .eyebrow {
    color: var(--color-brand-500);
    font-size: var(--text-xs);
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.08em;
  }
  h1 {
    margin-top: var(--space-2);
    font-size: var(--text-3xl);
    letter-spacing: -0.04em;
  }
  header p:last-child {
    margin-top: var(--space-2);
    color: var(--text-secondary);
  }
  .patient-pill {
    display: inline-flex;
    align-items: center;
    gap: var(--space-3);
    margin-top: var(--space-6);
    margin-bottom: var(--space-6);
    border-radius: var(--radius-pill);
    background: var(--surface-subtle);
    padding: 0.55rem 0.8rem 0.55rem 1rem;
    font-size: var(--text-sm);
    font-weight: 650;
  }
  .patient-pill select {
    border: 0;
    background: var(--surface-subtle);
    color: var(--text-secondary);
    font-size: var(--text-xs);
    cursor: pointer;
  }
  .list {
    display: grid;
    gap: var(--space-4);
  }
  .collection {
    margin-top: 0;
  }
  .list.grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .list.grid article {
    grid-template-columns: 1fr;
    align-items: start;
  }
  .list.grid .actions {
    flex-wrap: wrap;
  }
  article {
    display: grid;
    grid-template-columns: 1.1fr 1.5fr auto;
    align-items: center;
    gap: var(--space-6);
  }
  h3 {
    margin-top: var(--space-3);
  }
  article p {
    margin-top: var(--space-2);
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
  dl {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-5);
    margin: 0;
  }
  dt {
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
  dd {
    margin: 0.25rem 0 0;
    font-size: var(--text-sm);
    font-weight: 650;
  }
  .actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }
  .cancel {
    border: 0;
    background: transparent;
    color: var(--status-danger);
    font-size: var(--text-xs);
    font-weight: 700;
    cursor: pointer;
  }
  .dialog-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--space-3);
  }
  .dialog-field {
    display: grid;
    gap: var(--space-2);
    color: var(--text-primary);
    font-size: var(--text-sm);
    font-weight: 650;
  }
  .dialog-field select {
    min-height: 2.7rem;
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-sm);
    background: var(--surface-card);
    padding: 0 var(--space-3);
    color: var(--text-primary);
  }
  .dialog-field input {
    min-height: 2.7rem;
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-sm);
    background: var(--surface-card);
    padding: 0 var(--space-3);
    color: var(--text-primary);
  }
  .load-message {
    padding: var(--space-4);
    color: var(--text-secondary);
  }
  .load-message.error {
    color: var(--status-danger);
  }
  @media (max-width: 1000px) {
    .list.grid {
      grid-template-columns: 1fr;
    }
    article {
      grid-template-columns: 1fr 1fr;
    }
    .actions {
      grid-column: 1/-1;
    }
    dl {
      grid-template-columns: 1fr;
    }
  }
  @media (max-width: 680px) {
    .page {
      padding: var(--space-5);
    }
    header {
      align-items: flex-start;
      flex-direction: column;
    }
    article {
      grid-template-columns: 1fr;
    }
    .actions {
      grid-column: auto;
      flex-wrap: wrap;
    }
    .dialog-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
