<script lang="ts">
  import { onMount } from 'svelte';
  import Alert from '../../design-system/components/Alert.svelte';
  import Button from '../../design-system/components/Button.svelte';
  import Card from '../../design-system/components/Card.svelte';
  import PageHeader from '../../design-system/components/PageHeader.svelte';
  import CollectionPanel from '../../design-system/components/CollectionPanel.svelte';
  import ViewModeToggle from '../../design-system/components/ViewModeToggle.svelte';
  import { patientApi, type ApiVaccine } from '../../lib/api';
  let { onSchedule }: { onSchedule: (id: string) => void } = $props();
  let query = $state('');
  let category = $state('Todas');
  let vaccines = $state<ApiVaccine[]>([]);
  let loading = $state(true);
  let error = $state('');
  let viewMode = $state<'grid' | 'list'>((localStorage.getItem('orbe-view-vaccines') as 'grid' | 'list') ?? 'grid');
  let filtered = $derived(
    vaccines.filter(
      (v) => (category === 'Todas' || v.categoria === category) && v.nome.toLowerCase().includes(query.toLowerCase()),
    ),
  );
  let categories = $derived(['Todas', ...new Set(vaccines.map((v) => v.categoria))]);
  $effect(() => localStorage.setItem('orbe-view-vaccines', viewMode));
  onMount(async () => {
    try {
      vaccines = await patientApi.vaccines();
    } catch (exception) {
      error = exception instanceof Error ? exception.message : 'Não foi possível carregar as vacinas.';
    } finally {
      loading = false;
    }
  });
</script>

<div class="page">
  <PageHeader
    eyebrow="Vacinas"
    title="Catálogo de vacinas"
    description="Conheça as opções disponíveis e encontre a proteção ideal."
  />
  <div class="collection">
    <CollectionPanel title="Vacinas disponíveis" description={`${filtered.length} opções encontradas`}>
      {#snippet actions()}<ViewModeToggle bind:value={viewMode} />{/snippet}
      <div class="filters">
        <input aria-label="Buscar vacina" placeholder="Buscar por nome da vacina" bind:value={query} /><select
          aria-label="Filtrar categoria"
          bind:value={category}
          >{#each categories as item}<option>{item}</option>{/each}</select
        >
      </div>
      {#if error}<Alert tone="danger">{error}</Alert>{/if}
      {#if loading}<div class="empty"><p>Carregando vacinas...</p></div>{/if}
      <div class="grid {viewMode}">
        {#each filtered as vaccine}<Card padding="none"
            ><article>
              <div class="cover"><span>✚</span><small>{vaccine.categoria}</small></div>
              <div class="content">
                <div class="title">
                  <h2>{vaccine.nome}</h2>
                  <span class:unavailable={!vaccine.ativo}>{vaccine.ativo ? 'Disponível' : 'Indisponível'}</span
                  >
                </div>
                <p>{vaccine.descricao}</p>
                <dl>
                  <div>
                    <dt>Fabricante</dt>
                    <dd>{vaccine.fabricante}</dd>
                  </div>
                  <div>
                    <dt>Indicação</dt>
                    <dd>{vaccine.indicacao}</dd>
                  </div>
                  <div>
                    <dt>Esquema</dt>
                    <dd>{vaccine.esquemaDoses}</dd>
                  </div>
                </dl>
                <footer>
                  <strong>{vaccine.valorBase.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}</strong
                  ><Button size="sm" disabled={!vaccine.ativo} onclick={() => onSchedule(String(vaccine.id))}
                    >Agendar</Button
                  >
                </footer>
              </div>
            </article></Card
          >{/each}
      </div>
      {#if !loading && filtered.length === 0}<div class="empty">
          <span>⌕</span>
          <h2>Nenhuma vacina encontrada</h2>
          <p>Tente buscar outro nome ou remover o filtro selecionado.</p>
        </div>{/if}
    </CollectionPanel>
  </div>
</div>

<style>
  .page {
    width: min(100%, var(--content-max));
    margin: 0 auto;
    padding: var(--space-8);
  }
  .filters {
    display: flex;
    gap: var(--space-3);
    margin: 0 0 var(--space-6);
  }
  .collection {
    margin-top: var(--space-6);
  }
  input,
  select {
    min-height: 2.75rem;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: 0 var(--space-4);
    color: var(--text-primary);
  }
  input {
    width: min(100%, 25rem);
  }
  select {
    min-width: 13rem;
  }
  .grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    align-items: start;
    gap: var(--space-4);
  }
  .grid :global(.card) {
    align-self: start;
  }
  .grid.list {
    grid-template-columns: 1fr;
  }
  .grid.list article {
    display: grid;
    grid-template-columns: 11rem 1fr;
  }
  .grid.list .cover {
    min-height: 100%;
  }
  .cover {
    display: flex;
    min-height: 5.5rem;
    align-items: flex-start;
    justify-content: space-between;
    background: linear-gradient(135deg, var(--color-navy-900), var(--color-brand-700));
    padding: var(--space-4);
    color: white;
  }
  .cover > span {
    font-size: 1.6rem;
  }
  .cover small {
    border: 1px solid rgb(255 255 255/0.3);
    border-radius: var(--radius-pill);
    padding: 0.35rem 0.6rem;
  }
  .content {
    padding: var(--space-4);
  }
  .title {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-3);
  }
  h2 {
    font-size: var(--text-lg);
  }
  .title span {
    border-radius: var(--radius-pill);
    background: var(--status-success-bg);
    padding: 0.25rem 0.5rem;
    color: var(--status-success);
    font-size: 0.65rem;
    font-weight: 800;
  }
  .title span.unavailable {
    background: var(--surface-subtle);
    color: var(--text-secondary);
  }
  .content > p {
    margin-top: var(--space-2);
    color: var(--text-secondary);
    font-size: var(--text-sm);
    line-height: 1.4;
  }
  dl {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: var(--space-3);
    margin: var(--space-3) 0;
  }
  dt {
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
  dd {
    margin: 0.15rem 0 0;
    font-size: var(--text-sm);
    font-weight: 650;
  }
  footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-top: 1px solid var(--border-subtle);
    padding-top: var(--space-3);
  }
  footer strong {
    font-size: var(--text-lg);
  }
  .empty {
    display: grid;
    min-height: 14rem;
    place-items: center;
    place-content: center;
    gap: var(--space-2);
    text-align: center;
  }
  .empty span {
    font-size: 2rem;
    color: var(--color-brand-500);
  }
  .empty p {
    color: var(--text-secondary);
  }
  @media (max-width: 1100px) {
    .grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }
  @media (max-width: 680px) {
    .page {
      padding: var(--space-5);
    }
    .grid {
      grid-template-columns: 1fr;
    }
    .filters {
      align-items: stretch;
      flex-direction: column;
    }
    input,
    select {
      width: 100%;
    }
    dl {
      grid-template-columns: 1fr;
    }
  }
</style>
