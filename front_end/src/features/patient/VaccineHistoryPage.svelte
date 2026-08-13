<script lang="ts">
  import { onMount } from 'svelte';
  import Card from '../../design-system/components/Card.svelte';
  import Button from '../../design-system/components/Button.svelte';
  import PageHeader from '../../design-system/components/PageHeader.svelte';
  import CollectionPanel from '../../design-system/components/CollectionPanel.svelte';
  import ViewModeToggle from '../../design-system/components/ViewModeToggle.svelte';
  import { currentUser, patientApi, type ApiApplication } from '../../lib/api';
  import type { VaccineRecommendation } from '../../lib/patientRepository';
  let { onNavigate }: { onNavigate: (page: string) => void } = $props();

  const user = currentUser();
  let people = $state([{ id: String(user?.id ?? ''), name: user?.nome ?? 'Titular', relationship: 'Titular' }]);
  type HistoryItem = { id: string; vaccine: string; manufacturer: string; dose: string; date: string; batch: string; location: string; professional: string };
  let selectedPerson = $state(String(user?.id ?? ''));
  let selectedProfile = $derived(people.find((person) => person.id === selectedPerson) ?? people[0]);
  let history = $state<HistoryItem[]>([]);
  let recommendations = $state<VaccineRecommendation[]>([]);
  let loading = $state(true);
  let error = $state('');
  let nextRecommendation = $derived(recommendations[0]);
  let walletStatus = $derived(
    recommendations.some((item) => item.status === 'review')
      ? 'Revisão recomendada'
      : recommendations.length
        ? 'Acompanhamento ativo'
        : 'Sem pendências',
  );
  let viewMode = $state<'grid' | 'list'>((localStorage.getItem('orbe-view-history') as 'grid' | 'list') ?? 'list');
  $effect(() => localStorage.setItem('orbe-view-history', viewMode));
  function mapApplication(item: ApiApplication): HistoryItem {
    return { id: String(item.aplicacaoId), vaccine: item.vacina, manufacturer: item.fabricante,
      dose: item.dose, date: new Date(item.dataAplicacao).toLocaleDateString('pt-BR'),
      batch: item.numeroLote, location: item.localAplicacao, professional: item.profissional };
  }
  async function loadHistory() {
    const personId = Number(selectedPerson);
    if (!personId) return;
    loading = true; error = '';
    try {
      const isHolder = selectedPerson === String(user?.id ?? '');
      const [applications, apiRecommendations] = await Promise.all([
        isHolder ? patientApi.applications(personId) : patientApi.applicationsForDependent(personId),
        patientApi.recommendations(isHolder ? personId : undefined, isHolder ? undefined : personId),
      ]);
      history = applications.map(mapApplication);
      recommendations = apiRecommendations.map((item) => ({
        id: String(item.id), patientId: selectedPerson, vaccine: item.vacina, dose: item.dose,
        dueLabel: item.dataPrevista ? `Prevista para ${new Date(`${item.dataPrevista}T12:00:00`).toLocaleDateString('pt-BR')}` : 'Sem data definida',
        reason: item.motivo,
        status: item.status === 'AGENDADA' ? 'scheduled' : item.status === 'REVISAR' ? 'review' : 'recommended',
      }));
    }
    catch (exception) { error = exception instanceof Error ? exception.message : 'Não foi possível carregar a carteira.'; }
    finally { loading = false; }
  }
  onMount(async () => {
    try {
      const dependents = await patientApi.dependents();
      people = [people[0], ...dependents.map((item) => ({ id:String(item.id), name:item.nome, relationship:'Dependente' }))];
    } catch (exception) {
      error = exception instanceof Error ? exception.message : 'Não foi possível carregar os dependentes.';
    }
    await loadHistory();
  });
</script>

<div class="page">
  <PageHeader
    eyebrow="Saúde"
    title="Carteira vacinal"
    description="Registro digital das vacinas do titular e de seus dependentes."
  />

  <div class="person-selector" role="tablist" aria-label="Selecionar carteira">
    {#each people as person}
      <button class:active={selectedPerson === person.id} onclick={() => { selectedPerson = person.id; void loadHistory(); }}>
        <span>{person.name.slice(0, 1)}</span>
        <b>{person.name}<small>{person.relationship}</small></b>
      </button>
    {/each}
  </div>

  <div class="summary">
    <Card><strong>{history.length}</strong><span>Aplicações registradas</span></Card>
    <Card
      ><strong class="summary-text">{nextRecommendation?.vaccine ?? 'Nenhuma'}</strong><span
        >{nextRecommendation?.dueLabel ?? 'Sem recomendação cadastrada'}</span
      ></Card
    >
    <Card><strong class="summary-text">{walletStatus}</strong><span>Situação do acompanhamento</span></Card>
  </div>

  <div class="recommendations">
    <CollectionPanel
      title="Plano de próximas doses"
      description="Recomendações registradas pela clínica para esta pessoa"
    >
      <div class="recommendation-grid">
        {#each recommendations as item}
          <article class="recommendation">
            <div class="recommendation-head">
              <span class={item.status}
                >{item.status === 'scheduled' ? 'Agendada' : item.status === 'review' ? 'Revisar' : 'Recomendada'}</span
              ><small>{item.dueLabel}</small>
            </div>
            <h3>{item.vaccine}</h3>
            <p>{item.dose}</p>
            <div class="reason"><b>Por quê?</b><span>{item.reason}</span></div>
            <Button
              size="sm"
              variant={item.status === 'scheduled' ? 'secondary' : 'primary'}
              onclick={() =>
                onNavigate(
                  item.status === 'scheduled' ? 'appointments' : item.status === 'review' ? 'help' : 'booking',
                )}
              >{item.status === 'scheduled'
                ? 'Ver agendamento'
                : item.status === 'review'
                  ? 'Consultar a clínica'
                  : 'Agendar vacina'}</Button
            >
          </article>
        {/each}
        {#if recommendations.length === 0}<div class="empty">
            <h3>Nenhuma recomendação pendente</h3>
            <p>Novas orientações da clínica aparecerão aqui.</p>
          </div>{/if}
      </div>
      {#snippet footer()}<p class="disclaimer">
          As recomendações são informativas e devem ser confirmadas por um profissional de saúde.
        </p>{/snippet}
    </CollectionPanel>
  </div>

  <div class="collection">
    <CollectionPanel title={`Aplicações de ${selectedProfile.name}`} description="Histórico individual de vacinação">
      {#snippet actions()}<ViewModeToggle bind:value={viewMode} />{/snippet}
      <div class="patient">
        <div>{selectedProfile.name.slice(0, 1)}</div>
        <span
          ><strong>{selectedProfile.name}</strong><small>{selectedProfile.relationship} · Carteira individual</small
          ></span
        >
        <button onclick={() => window.print()}>Imprimir carteira ↓</button>
      </div>
      <div class="timeline {viewMode}">
        {#if error}<div class="empty"><p>{error}</p></div>{/if}
        {#if loading}<div class="empty"><p>Carregando carteira vacinal...</p></div>{/if}
        {#each history as item}
          <article>
            <span class="marker">✓</span>
            <div>
              <small>{item.date}</small>
              <h2>{item.vaccine}</h2>
              <p>{item.dose} · {item.manufacturer}</p>
              <dl>
                <div>
                  <dt>Lote</dt>
                  <dd>{item.batch}</dd>
                </div>
                <div>
                  <dt>Unidade</dt>
                  <dd>{item.location}</dd>
                </div>
                <div>
                  <dt>Profissional</dt>
                  <dd>{item.professional}</dd>
                </div>
              </dl>
            </div>
            <button aria-label={`Imprimir comprovante de ${item.vaccine}`} onclick={() => window.print()}>›</button>
          </article>
        {/each}
        {#if !loading && history.length === 0}<div class="empty">
            <h2>Nenhuma aplicação registrada</h2>
            <p>As vacinas aplicadas aparecerão aqui.</p>
          </div>{/if}
      </div>
    </CollectionPanel>
  </div>
</div>

<style>
  .page {
    width: min(100%, var(--content-max));
    margin: 0 auto;
    padding: var(--space-8);
  }
  .person-selector {
    display: flex;
    gap: var(--space-3);
    margin-top: var(--space-6);
    overflow-x: auto;
    padding-bottom: var(--space-1);
  }
  .person-selector button {
    display: flex;
    min-width: 12rem;
    align-items: center;
    gap: var(--space-3);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: var(--space-3);
    color: var(--text-primary);
    text-align: left;
    cursor: pointer;
  }
  .person-selector button.active {
    border-color: var(--color-brand-500);
    box-shadow: 0 0 0 2px var(--focus-ring);
  }
  .person-selector button > span {
    display: grid;
    width: 2.1rem;
    height: 2.1rem;
    place-items: center;
    border-radius: 50%;
    background: var(--color-brand-50);
    color: var(--color-brand-600);
    font-weight: 800;
  }
  .person-selector b {
    display: grid;
    font-size: var(--text-xs);
  }
  .person-selector small {
    margin-top: 0.15rem;
    color: var(--text-secondary);
    font-weight: 400;
  }
  .summary {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-4);
    margin-top: var(--space-6);
  }
  .summary :global(.card) {
    display: grid;
    gap: var(--space-2);
  }
  .summary strong {
    font-size: var(--text-2xl);
  }
  .summary strong.summary-text {
    font-size: var(--text-lg);
    line-height: 1.25;
  }
  .summary span {
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
  .recommendations {
    margin-top: var(--space-6);
  }
  .recommendation-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-4);
  }
  .recommendation {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-4);
  }
  .recommendation-head {
    display: flex;
    width: 100%;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
  }
  .recommendation-head > span {
    border-radius: var(--radius-pill);
    padding: 0.3rem 0.6rem;
    font-size: var(--text-xs);
    font-weight: 800;
  }
  .recommendation-head .scheduled {
    background: var(--status-success-bg);
    color: var(--status-success);
  }
  .recommendation-head .recommended {
    background: var(--color-brand-50);
    color: var(--color-brand-700);
  }
  .recommendation-head .review {
    background: var(--status-warning-bg);
    color: var(--status-warning);
  }
  .recommendation-head small {
    color: var(--text-secondary);
    text-align: right;
  }
  .recommendation h3 {
    margin-top: var(--space-3);
    font-size: var(--text-md);
  }
  .recommendation > p {
    margin-top: 0.25rem;
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
  .reason {
    display: grid;
    gap: 0.2rem;
    margin: var(--space-3) 0;
    color: var(--text-secondary);
    font-size: var(--text-xs);
  }
  .reason b {
    color: var(--text-primary);
  }
  .recommendation :global(.button) {
    margin-top: var(--space-1);
  }
  .disclaimer {
    color: var(--text-secondary);
    font-size: var(--text-xs);
    text-align: center;
  }
  .collection {
    margin-top: var(--space-6);
  }
  .patient {
    display: flex;
    align-items: center;
    gap: var(--space-4);
    border-bottom: 1px solid var(--border-subtle);
    padding: var(--space-5);
  }
  .patient > div {
    display: grid;
    width: 2.7rem;
    height: 2.7rem;
    place-items: center;
    border-radius: 50%;
    background: var(--color-brand-500);
    color: white;
    font-weight: 800;
  }
  .patient span {
    display: grid;
    gap: 0.2rem;
  }
  .patient small {
    color: var(--text-secondary);
  }
  .patient button {
    margin-left: auto;
    border: 0;
    background: transparent;
    color: var(--color-brand-500);
    font-size: var(--text-sm);
    font-weight: 700;
    cursor: pointer;
  }
  .timeline {
    padding: var(--space-3) var(--space-6);
  }
  .timeline.grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-4);
    padding: var(--space-5) 0 0;
  }
  .timeline.grid article {
    grid-template-columns: auto 1fr;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-5);
  }
  .timeline.grid article > button {
    display: none;
  }
  .timeline.grid dl {
    flex-direction: column;
    gap: var(--space-3);
  }
  article {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: var(--space-5);
    padding: var(--space-6) 0;
    border-bottom: 1px solid var(--border-subtle);
  }
  article:last-child {
    border-bottom: 0;
  }
  .marker {
    display: grid;
    width: 2rem;
    height: 2rem;
    place-items: center;
    border-radius: 50%;
    background: var(--status-success-bg);
    color: var(--status-success);
    font-weight: 800;
  }
  article small {
    color: var(--text-tertiary);
  }
  article h2 {
    margin-top: var(--space-2);
    font-size: var(--text-lg);
  }
  article p {
    margin-top: 0.25rem;
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
  dl {
    display: flex;
    gap: var(--space-8);
    margin: var(--space-5) 0 0;
  }
  dt {
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
  dd {
    margin: 0.2rem 0 0;
    font-size: var(--text-sm);
    font-weight: 650;
  }
  article > button {
    border: 0;
    background: transparent;
    color: var(--text-tertiary);
    font-size: 1.6rem;
    cursor: pointer;
  }
  .empty {
    padding: var(--space-8);
    text-align: center;
  }
  .empty p {
    margin-top: var(--space-2);
    color: var(--text-secondary);
  }
  @media (max-width: 680px) {
    .page {
      padding: var(--space-5);
    }
    .summary {
      grid-template-columns: 1fr;
    }
    .timeline {
      padding: var(--space-2) var(--space-4);
    }
    dl {
      align-items: flex-start;
      flex-direction: column;
      gap: var(--space-3);
    }
    .patient button {
      display: none;
    }
  }
  @media (max-width: 850px) {
    .timeline.grid {
      grid-template-columns: 1fr;
    }
    .recommendation-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
