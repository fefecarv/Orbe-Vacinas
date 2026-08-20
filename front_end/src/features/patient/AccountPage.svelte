<script lang="ts">
  import { onMount } from 'svelte';
  import Alert from '../../design-system/components/Alert.svelte';
  import Button from '../../design-system/components/Button.svelte';
  import Card from '../../design-system/components/Card.svelte';
  import FormField from '../../design-system/components/FormField.svelte';
  import PageHeader from '../../design-system/components/PageHeader.svelte';
  import Toast from '../../design-system/components/Toast.svelte';
  import CollectionPanel from '../../design-system/components/CollectionPanel.svelte';
  import ViewModeToggle from '../../design-system/components/ViewModeToggle.svelte';
  import PortalRecordDialog from './PortalRecordDialog.svelte';
  import { dependents as seedDependents, insurances as seedInsurances } from '../../mocks/portal';
  import { patientApi, type ApiUserProfile } from '../../lib/api';

  let { mode }: { mode: 'family' | 'insurance' | 'profile' } = $props();
  type Row = Record<string, string>;
  const read = (key: string, fallback: Row[]) => {
    try {
      return JSON.parse(localStorage.getItem(key) ?? 'null') ?? fallback;
    } catch {
      return fallback;
    }
  };
  let family = $state<Row[]>(
    read(
      'orbe-portal-dependents',
      seedDependents.map((item) => ({ ...item, active: 'true' })),
    ),
  );
  let insurance = $state<Row[]>(
    read(
      'orbe-portal-insurance',
      seedInsurances.map((item) => ({ ...item, active: String(item.active) })),
    ),
  );
  let dialog = $state<'dependent' | 'insurance' | null>(null);
  let editing = $state<Row | null>(null);
  let toast = $state('');
  let cardView = $state<Row | null>(null);
  let loadError = $state('');
  let acceptedInsurances = $state<Row[]>([]);
  let profile = $state<ApiUserProfile>({
    id: 0,
    nome: '',
    cpf: '',
    email: '',
    telefone: '',
    dataNascimento: '',
    cep: '',
    logradouro: '',
    numero: '',
    complemento: '',
    bairro: '',
    cidade: '',
    estado: '',
  });
  let savingProfile = $state(false);
  let viewMode = $state<'grid' | 'list'>((localStorage.getItem('orbe-view-account') as 'grid' | 'list') ?? 'grid');
  const metadata = {
    family: {
      eyebrow: 'Família',
      title: 'Meus dependentes',
      description: 'Gerencie as pessoas vinculadas à sua conta.',
    },
    insurance: {
      eyebrow: 'Atendimento',
      title: 'Meus convênios',
      description: 'Mantenha os dados dos seus planos de saúde atualizados.',
    },
    profile: {
      eyebrow: 'Minha conta',
      title: 'Dados cadastrais',
      description: 'Revise suas informações pessoais e de contato.',
    },
  };
  $effect(() => localStorage.setItem('orbe-portal-dependents', JSON.stringify(family)));
  $effect(() => localStorage.setItem('orbe-portal-insurance', JSON.stringify(insurance)));
  $effect(() => localStorage.setItem('orbe-view-account', viewMode));

  onMount(async () => {
    try {
      if (mode === 'family') {
        const dependents = await patientApi.dependents();
        family = dependents.map((item) => ({
        id: String(item.id),
        name: item.nome,
        relationship: 'Dependente',
        birthDate: new Date(`${item.dataNascimento}T00:00:00`).toLocaleDateString('pt-BR'),
        cpf: 'Não informado',
        active: String(item.status === 'ATIVO'),
        }));
      } else if (mode === 'insurance') {
        const [cards, accepted] = await Promise.all([patientApi.insurances(), patientApi.acceptedInsurances()]);
        acceptedInsurances = accepted.map((item) => ({id:String(item.id),label:`${item.nome} · ${item.plano}`}));
        insurance = cards.map((item) => ({id:String(item.id),convenioId:String(item.convenioId),company:item.nomeConvenio,plan:item.plano,cardNumber:item.numeroCarteirinha,holder:item.titular,validUntil:item.dataValidade,active:String(item.ativo)}));
      } else if (mode === 'profile') {
        profile = await patientApi.profile();
      }
    } catch (exception) {
      loadError = exception instanceof Error ? exception.message : 'Não foi possível carregar os dependentes.';
    }
  });

  async function saveRecord(values: Row) {
    if (dialog === 'dependent')
      family = family.some((item) => item.id === values.id)
        ? family.map((item) => (item.id === values.id ? values : item))
        : [values, ...family];
    else {
      await patientApi.saveInsurance({id:editing?Number(values.id):undefined,convenioId:Number(values.convenioId),numeroCarteirinha:values.cardNumber,titular:values.holder,dataValidade:values.validUntil});
      const cards=await patientApi.insurances();
      insurance=cards.map((item)=>({id:String(item.id),convenioId:String(item.convenioId),company:item.nomeConvenio,plan:item.plano,cardNumber:item.numeroCarteirinha,holder:item.titular,validUntil:item.dataValidade,active:String(item.ativo)}));
    }
    toast = editing ? 'Registro atualizado com sucesso.' : 'Registro cadastrado com sucesso.';
    dialog = null;
    editing = null;
  }

  async function saveProfile() {
    savingProfile = true;
    loadError = '';
    try {
      profile = await patientApi.saveProfile(profile);
      toast = 'Dados cadastrais salvos com sucesso.';
    } catch (exception) {
      loadError = exception instanceof Error ? exception.message : 'Não foi possível salvar seus dados.';
    } finally {
      savingProfile = false;
    }
  }
</script>

<div class="page">
  <PageHeader {...metadata[mode]}>
    {#snippet actions()}
      {#if mode === 'family'}<Button
          onclick={() => {
            editing = null;
            dialog = 'dependent';
          }}>Adicionar dependente</Button
        >
      {:else if mode === 'insurance'}<Button
          onclick={() => {
            editing = null;
            dialog = 'insurance';
          }}>Adicionar convênio</Button
        >{/if}
    {/snippet}
  </PageHeader>

  {#if loadError}<Alert tone="danger">{loadError}</Alert>{/if}

  {#if mode === 'family'}
    <div class="collection">
      <CollectionPanel title="Pessoas vinculadas" description={`${family.length} registros`}>
        {#snippet actions()}<ViewModeToggle bind:value={viewMode} />{/snippet}
        <div class="cards {viewMode}">
          {#each family as person}<Card
              ><article>
                <div class="avatar">{person.name.slice(0, 1)}</div>
                <div>
                  <h2>{person.name}</h2>
                  <p>{person.relationship} · {person.active === 'true' ? 'Ativo' : 'Inativo'}</p>
                  <dl>
                    <div>
                      <dt>Nascimento</dt>
                      <dd>{person.birthDate}</dd>
                    </div>
                    <div>
                      <dt>CPF</dt>
                      <dd>{person.cpf}</dd>
                    </div>
                  </dl>
                  <button
                    class="link"
                    onclick={() => {
                      editing = person;
                      dialog = 'dependent';
                    }}>Editar dados</button
                  >
                </div>
              </article></Card
            >{/each}
        </div></CollectionPanel
      >
    </div>
  {:else if mode === 'insurance'}
    <div class="collection">
      <CollectionPanel title="Planos cadastrados" description={`${insurance.length} registros`}>
        {#snippet actions()}<ViewModeToggle bind:value={viewMode} />{/snippet}
        <div class="cards {viewMode}">
          {#each insurance as item}<Card
              ><article>
                <div class="head">
                  <div class="avatar">◇</div>
                  <span>{item.active === 'true' ? 'Ativo' : 'Inativo'}</span>
                </div>
                <h2>{item.company}</h2>
                <p>{item.plan}</p>
                <dl>
                  <div>
                    <dt>Carteirinha</dt>
                    <dd>{item.cardNumber}</dd>
                  </div>
                  <div>
                    <dt>Titular</dt>
                    <dd>{item.holder}</dd>
                  </div>
                  <div>
                    <dt>Validade</dt>
                    <dd>{item.validUntil}</dd>
                  </div>
                </dl>
                <footer>
                  <button
                    onclick={() => {
                      editing = item;
                      dialog = 'insurance';
                    }}>Editar dados</button
                  ><button onclick={() => (cardView = item)}>Ver carteirinha</button>
                </footer>
              </article></Card
            >{/each}
        </div></CollectionPanel
      >
    </div>
  {:else}
    <form
      onsubmit={async (event) => {
        event.preventDefault();
        await saveProfile();
      }}
    >
      <Card padding="lg"
        ><h2>Informações pessoais</h2>
        <div class="form-grid">
          <FormField id="profile-name" label="Nome completo" value={profile.nome} oninput={(value) => (profile.nome = value)} required /><FormField
            id="profile-cpf"
            label="CPF"
            value={profile.cpf}
            disabled
          /><FormField id="profile-birth" label="Data de nascimento" type="date" value={profile.dataNascimento} oninput={(value) => (profile.dataNascimento = value)} required /><FormField
            id="profile-phone"
            label="Telefone"
            type="tel"
            value={profile.telefone}
            oninput={(value) => (profile.telefone = value)}
            required
          /><FormField id="profile-email" label="E-mail" type="email" value={profile.email} oninput={(value) => (profile.email = value)} required />
        </div></Card
      >
      <Card padding="lg"
        ><h2>Endereço</h2>
        <div class="form-grid">
          <FormField id="cep" label="CEP" value={profile.cep ?? ''} oninput={(value) => (profile.cep = value)} /><FormField
            id="street"
            label="Logradouro"
            value={profile.logradouro ?? ''}
            oninput={(value) => (profile.logradouro = value)}
          /><FormField id="number" label="Número" value={profile.numero ?? ''} oninput={(value) => (profile.numero = value)} /><FormField
            id="district"
            label="Bairro"
            value={profile.bairro ?? ''}
            oninput={(value) => (profile.bairro = value)}
          /><FormField id="city" label="Cidade" value={profile.cidade ?? ''} oninput={(value) => (profile.cidade = value)} /><FormField id="state" label="Estado" value={profile.estado ?? ''} oninput={(value) => (profile.estado = value)} />
          <FormField id="complement" label="Complemento" value={profile.complemento ?? ''} oninput={(value) => (profile.complemento = value)} />
        </div></Card
      >
      <div class="save"><Button type="submit" disabled={savingProfile}>{savingProfile ? 'Salvando…' : 'Salvar alterações'}</Button></div>
    </form>
  {/if}
</div>

{#if dialog}<PortalRecordDialog
    type={dialog}
    initial={editing ?? {}}
    insuranceOptions={acceptedInsurances}
    onSave={saveRecord}
    onCancel={() => {
      dialog = null;
      editing = null;
    }}
  />{/if}
{#if cardView}<div
    class="overlay"
    role="presentation"
    onclick={(event) => event.target === event.currentTarget && (cardView = null)}
  >
    <div class="health-card" role="dialog" aria-modal="true">
      <small>CARTEIRA DO CONVÊNIO</small>
      <h2>{cardView.company}</h2>
      <p>{cardView.plan}</p>
      <strong>{cardView.cardNumber}</strong><span>{cardView.holder} · Val. {cardView.validUntil}</span><Button
        variant="secondary"
        onclick={() => (cardView = null)}>Fechar</Button
      >
    </div>
  </div>{/if}
{#if toast}<Toast message={toast} onClose={() => (toast = '')} />{/if}

<style>
  .page {
    width: min(100%, var(--content-max));
    margin: 0 auto;
    padding: var(--space-8);
  }
  .cards {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-4);
  }
  .collection {
    margin-top: var(--space-6);
  }
  .cards.list {
    grid-template-columns: 1fr;
  }
  article {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: var(--space-4);
  }
  .avatar {
    display: grid;
    width: 3rem;
    height: 3rem;
    place-items: center;
    border-radius: 50%;
    background: var(--color-brand-50);
    color: var(--color-brand-600);
    font-weight: 800;
  }
  h2 {
    font-size: var(--text-lg);
  }
  article p {
    margin-top: 0.25rem;
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
  dl {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-4);
    margin: var(--space-5) 0;
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
  .link,
  footer button {
    border: 0;
    background: transparent;
    padding: 0;
    color: var(--color-brand-500);
    font-size: var(--text-sm);
    font-weight: 700;
    cursor: pointer;
  }
  .head {
    grid-column: 1/-1;
    display: flex;
    justify-content: space-between;
  }
  .head span {
    align-self: flex-start;
    border-radius: var(--radius-pill);
    background: var(--status-success-bg);
    padding: 0.3rem 0.6rem;
    color: var(--status-success);
    font-size: var(--text-xs);
  }
  article > .head ~ * {
    grid-column: 1/-1;
  }
  footer {
    display: flex;
    gap: var(--space-5);
    border-top: 1px solid var(--border-subtle);
    padding-top: var(--space-4);
  }
  form {
    display: grid;
    gap: var(--space-4);
    margin-top: var(--space-6);
  }
  form h2 {
    margin-bottom: var(--space-5);
  }
  .form-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-5);
  }
  .save {
    display: flex;
    justify-content: flex-end;
  }
  .overlay {
    position: fixed;
    z-index: 90;
    inset: 0;
    display: grid;
    place-items: center;
    background: var(--surface-overlay);
    padding: var(--space-5);
  }
  .health-card {
    display: grid;
    width: min(100%, 28rem);
    gap: var(--space-3);
    border-radius: var(--radius-xl);
    background: linear-gradient(135deg, var(--color-brand-700), var(--color-brand-500));
    padding: var(--space-6);
    color: white;
    box-shadow: var(--shadow-md);
  }
  .health-card strong {
    margin-top: var(--space-6);
    font-size: var(--text-xl);
    letter-spacing: 0.08em;
  }
  .health-card span {
    margin-bottom: var(--space-5);
    font-size: var(--text-sm);
  }
  @media (max-width: 750px) {
    .page {
      padding: var(--space-5);
    }
    .cards,
    .form-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
