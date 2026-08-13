<script lang="ts">
  import { untrack } from 'svelte';
  import Button from '../../design-system/components/Button.svelte';
  import FormField from '../../design-system/components/FormField.svelte';

  let {
    type,
    initial = {},
    onSave,
    onCancel,
    insuranceOptions = [],
  }: {
    type: 'dependent' | 'insurance';
    initial?: Record<string, string>;
    onSave: (values: Record<string, string>) => void;
    onCancel: () => void;
    insuranceOptions?: Record<string,string>[];
  } = $props();
  let values = $state<Record<string, string>>(untrack(() => ({ ...initial })));
  let error = $state('');
  let fields = $derived(
    type === 'dependent'
      ? [
          ['name', 'Nome completo'],
          ['relationship', 'Parentesco'],
          ['birthDate', 'Data de nascimento'],
          ['cpf', 'CPF'],
        ]
      : [
          ['cardNumber', 'Número da carteirinha'],
          ['holder', 'Titular'],
          ['validUntil', 'Validade'],
        ],
  );

  function submit(event: SubmitEvent) {
    event.preventDefault();
    if ((type === 'insurance' && !values.convenioId?.trim()) || fields.some(([key]) => !values[key]?.trim())) {
      error = 'Preencha todos os campos obrigatórios.';
      return;
    }
    onSave({ ...values, id: initial.id ?? `${type}-${Date.now()}`, active: values.active ?? 'true' });
  }
</script>

<div class="backdrop" role="presentation" onclick={(event) => event.target === event.currentTarget && onCancel()}>
  <div role="dialog" aria-modal="true" aria-labelledby="record-title">
    <form onsubmit={submit}>
      <header>
        <div>
          <p>{initial.id ? 'Editar' : 'Cadastrar'}</p>
          <h2 id="record-title">{type === 'dependent' ? 'Dependente' : 'Convênio'}</h2>
        </div>
        <button type="button" onclick={onCancel} aria-label="Fechar">×</button>
      </header>
      {#if error}<div class="error" role="alert">{error}</div>{/if}
      <div class="grid">
        {#if type === 'insurance'}<label>Plano aceito<select value={values.convenioId ?? ''} onchange={(event)=>(values.convenioId=event.currentTarget.value)} required><option value="">Selecione</option>{#each insuranceOptions as option}<option value={option.id}>{option.label}</option>{/each}</select></label>{/if}
        {#each fields as field}
          <FormField
            id={`portal-${field[0]}`}
            label={field[1]}
            value={values[field[0]] ?? ''}
            oninput={(value) => (values[field[0]] = value)}
            required
          />
        {/each}
      </div>
      <footer>
        <Button variant="secondary" onclick={onCancel}>Cancelar</Button><Button type="submit">Salvar</Button>
      </footer>
    </form>
  </div>
</div>

<style>
  .backdrop {
    position: fixed;
    z-index: 90;
    inset: 0;
    display: grid;
    place-items: center;
    background: var(--surface-overlay);
    padding: var(--space-5);
  }
  form {
    width: min(100%, 38rem);
    max-height: calc(100vh - 2rem);
    overflow: auto;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-lg);
    background: var(--surface-card);
    padding: var(--space-6);
    box-shadow: var(--shadow-md);
  }
  header {
    display: flex;
    justify-content: space-between;
    margin-bottom: var(--space-5);
  }
  header p {
    color: var(--color-brand-500);
    font-size: var(--text-xs);
    font-weight: 800;
    text-transform: uppercase;
  }
  h2 {
    margin-top: 0.2rem;
    font-size: var(--text-xl);
  }
  header button {
    border: 0;
    background: transparent;
    color: var(--text-secondary);
    font-size: 1.5rem;
    cursor: pointer;
  }
  .grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--space-4);
  }
  label {
    display: grid;
    gap: var(--space-2);
    font-size: var(--text-sm);
    font-weight: 650;
  }
  select {
    min-height: 2.875rem;
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: 0 var(--space-3);
    color: var(--text-primary);
  }
  .error {
    margin-bottom: var(--space-4);
    border-radius: var(--radius-sm);
    background: var(--status-danger-bg);
    padding: var(--space-3);
    color: var(--status-danger);
    font-size: var(--text-sm);
  }
  footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-6);
    border-top: 1px solid var(--border-subtle);
    padding-top: var(--space-5);
  }
  @media (max-width: 600px) {
    .grid {
      grid-template-columns: 1fr;
    }
  }
</style>
