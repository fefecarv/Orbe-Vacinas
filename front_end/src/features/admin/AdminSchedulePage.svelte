<script lang="ts">
  import { onMount } from 'svelte';
  import PageHeader from '../../design-system/components/PageHeader.svelte';
  import Card from '../../design-system/components/Card.svelte';
  import Button from '../../design-system/components/Button.svelte';
  import Toast from '../../design-system/components/Toast.svelte';
  import { adminApi } from '../../lib/api';
  const days = ['Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado', 'Domingo'];
  let schedules = $state<Array<any>>([]),
    blocks = $state<Array<any>>([]),
    toast = $state('');
  let block = $state({ unidade: 'Orbe Centro', dataBloqueio: '', horaInicio: '', horaFim: '', motivo: '' });
  async function load() {
    [schedules, blocks] = await Promise.all([adminApi.schedule(), adminApi.blocks()]);
  }
  async function save(item: any) {
    await adminApi.saveSchedule(item);
    toast = 'Horário de funcionamento atualizado.';
    await load();
  }
  async function addBlock() {
    await adminApi.saveBlock({ ...block, horaInicio: block.horaInicio || null, horaFim: block.horaFim || null });
    toast = 'Exceção cadastrada.';
    block = { unidade: 'Orbe Centro', dataBloqueio: '', horaInicio: '', horaFim: '', motivo: '' };
    await load();
  }
  onMount(load);
</script>

<div class="page">
  <PageHeader
    eyebrow="Agenda"
    title="Funcionamento da clínica"
    description="Defina os dias, horários e exceções usados na marcação de vacinas."
  />
  <div class="layout">
    <Card
      ><h2>Semana padrão</h2>
      <div class="days">
        {#each days as name, index}{@const item = schedules.find((x) => x.diaSemana === index + 1) ?? {
            unidade: 'Orbe Centro',
            diaSemana: index + 1,
            horaAbertura: '08:00',
            horaFechamento: '17:00',
            intervaloMinutos: 30,
            ativo: false,
          }}
          <article>
            <label class="toggle"><input type="checkbox" bind:checked={item.ativo} /><strong>{name}</strong></label
            ><label>Abre<input type="time" bind:value={item.horaAbertura} /></label><label
              >Fecha<input type="time" bind:value={item.horaFechamento} /></label
            ><label
              >Intervalo<select bind:value={item.intervaloMinutos}
                ><option value={15}>15 min</option><option value={30}>30 min</option><option value={60}>60 min</option
                ></select
              ></label
            ><Button size="sm" onclick={() => save(item)}>Salvar</Button>
          </article>{/each}
      </div></Card
    >
    <Card
      ><h2>Fechamentos e horários especiais</h2>
      <form
        onsubmit={(e) => {
          e.preventDefault();
          addBlock();
        }}
      >
        <label>Data<input type="date" required bind:value={block.dataBloqueio} /></label>
        <div class="pair">
          <label>Das (opcional)<input type="time" bind:value={block.horaInicio} /></label><label
            >Até<input type="time" bind:value={block.horaFim} /></label
          >
        </div>
        <label>Motivo<input required placeholder="Feriado, reunião..." bind:value={block.motivo} /></label><Button
          type="submit">Adicionar bloqueio</Button
        >
      </form>
      <div class="blocks">
        {#each blocks as b}<p>
            <strong>{new Date(`${b.dataBloqueio}T12:00`).toLocaleDateString('pt-BR')}</strong><span
              >{b.horaInicio ? `${b.horaInicio.slice(0, 5)}–${b.horaFim.slice(0, 5)}` : 'Dia inteiro'} · {b.motivo}</span
            >
          </p>{/each}
      </div></Card
    >
  </div>
</div>
{#if toast}<Toast message={toast} onClose={() => (toast = '')} />{/if}

<style>
  .page {
    width: min(100%, var(--content-max));
    margin: auto;
    padding: var(--space-8);
  }
  .layout {
    display: grid;
    grid-template-columns: 1.4fr 1fr;
    gap: var(--space-4);
    margin-top: var(--space-6);
  }
  h2 {
    font-size: var(--text-lg);
    margin-bottom: var(--space-4);
  }
  .days {
    display: grid;
  }
  .days article {
    display: grid;
    grid-template-columns: 1.5fr repeat(3, 1fr) auto;
    align-items: end;
    gap: var(--space-3);
    border-top: 1px solid var(--border-subtle);
    padding: var(--space-3) 0;
  }
  label {
    display: grid;
    gap: 0.3rem;
    color: var(--text-secondary);
    font-size: var(--text-xs);
  }
  .toggle {
    display: flex;
    align-items: center;
    color: var(--text-primary);
  }
  input,
  select {
    min-height: 2.5rem;
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-sm);
    background: var(--surface-card);
    padding: 0 var(--space-2);
    color: var(--text-primary);
  }
  form {
    display: grid;
    gap: var(--space-3);
  }
  .pair {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--space-3);
  }
  .blocks {
    margin-top: var(--space-5);
  }
  .blocks p {
    display: grid;
    gap: 0.2rem;
    border-top: 1px solid var(--border-subtle);
    padding: var(--space-3) 0;
    font-size: var(--text-xs);
  }
  .blocks span {
    color: var(--text-secondary);
  }
  @media (max-width: 1050px) {
    .layout {
      grid-template-columns: 1fr;
    }
    .days article {
      grid-template-columns: 1fr 1fr 1fr;
    }
  }
  @media (max-width: 680px) {
    .page {
      padding: var(--space-5);
    }
    .days article {
      grid-template-columns: 1fr 1fr;
    }
    .toggle {
      grid-column: 1/-1;
    }
  }
</style>
