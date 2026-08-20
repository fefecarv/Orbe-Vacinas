<script lang="ts">
  import { untrack } from 'svelte';
  import Button from '../../design-system/components/Button.svelte';
  import FormField from '../../design-system/components/FormField.svelte';
  import type { StaffPatient, StaffPatientInput } from '../../lib/api';

  let {
    initial,
    holders,
    onSave,
    onCancel,
  }: {
    initial?: StaffPatient;
    holders: StaffPatient[];
    onSave: (patient: StaffPatientInput, id?: string) => void;
    onCancel: () => void;
  } = $props();

  let values = $state(
    untrack(() => ({
      tipo: initial?.tipo ?? ('TITULAR' as 'TITULAR' | 'DEPENDENTE'),
      nome: initial?.nome ?? '',
      cpf: initial?.cpf ?? '',
      dataNascimento: initial?.dataNascimento ?? '',
      telefone: initial?.telefone ?? '',
      email: initial?.email ?? '',
      senhaTemporaria: '',
      cep: initial?.cep ?? '',
      logradouro: initial?.logradouro ?? '',
      numero: initial?.numero ?? '',
      complemento: initial?.complemento ?? '',
      bairro: initial?.bairro ?? '',
      cidade: initial?.cidade ?? '',
      estado: initial?.estado ?? '',
      status: initial?.status ?? 'ATIVO',
      responsavelId: initial?.responsavelId ?? null,
      parentesco: initial?.parentesco ?? '',
    })),
  );
  let error = $state('');

  function submit(event: SubmitEvent) {
    event.preventDefault();
    const cpf = values.cpf.replace(/\D/g, '');
    const invalidCpf = values.tipo === 'TITULAR' ? cpf.length !== 11 : cpf.length > 0 && cpf.length !== 11;
    if (
      !values.nome ||
      invalidCpf ||
      !values.dataNascimento ||
      (values.tipo === 'TITULAR' &&
        (!values.telefone || !values.email || (!initial && values.senhaTemporaria.length < 8))) ||
      (values.tipo === 'DEPENDENTE' && (!values.responsavelId || !values.parentesco))
    ) {
      error = 'Revise os campos obrigatórios e os dados informados.';
      return;
    }
    onSave({ ...values, cpf, status: initial ? values.status : 'ATIVO' }, initial?.id);
  }
</script>

<div class="backdrop" role="presentation" onclick={(event) => event.target === event.currentTarget && onCancel()}>
  <div role="dialog" aria-modal="true" aria-labelledby="patient-title">
    <form onsubmit={submit}>
      <header>
        <div>
          <p>Cadastro clínico</p>
          <h2 id="patient-title">{initial ? 'Editar paciente' : 'Novo paciente'}</h2>
        </div>
        <button type="button" aria-label="Fechar" onclick={onCancel}>×</button>
      </header>
      {#if error}<div class="error" role="alert">{error}</div>{/if}
      <section>
        <h3>Identificação</h3>
        <div class="grid">
          <label
            >Tipo<select bind:value={values.tipo} disabled={!!initial}
              ><option value="TITULAR">Titular</option><option value="DEPENDENTE">Dependente</option></select
            ></label
          >
          <FormField
            id="patient-name"
            label="Nome completo"
            value={values.nome}
            oninput={(value) => (values.nome = value)}
            required
          />
          <FormField
            id="patient-cpf"
            label={values.tipo === 'DEPENDENTE' ? 'CPF (opcional)' : 'CPF'}
            value={values.cpf}
            oninput={(value) => (values.cpf = value)}
            required={values.tipo === 'TITULAR'}
          />
          <FormField
            id="patient-birth"
            label="Data de nascimento"
            type="date"
            value={values.dataNascimento}
            oninput={(value) => (values.dataNascimento = value)}
            required
          />
        </div>
      </section>
      {#if values.tipo === 'TITULAR'}
        <section>
          <h3>Acesso e contato</h3>
          <div class="grid">
            <FormField
              id="patient-phone"
              label="Telefone"
              type="tel"
              value={values.telefone}
              oninput={(value) => (values.telefone = value)}
              required
            />
            <FormField
              id="patient-email"
              label="E-mail"
              type="email"
              value={values.email}
              oninput={(value) => (values.email = value)}
              required
            />
            {#if !initial}<FormField
                id="patient-password"
                label="Senha temporária"
                type="password"
                value={values.senhaTemporaria}
                oninput={(value) => (values.senhaTemporaria = value)}
                required
              />
              <p class="hint">O paciente deverá trocar esta senha no primeiro acesso.</p>{/if}
          </div>
        </section>
        <section>
          <h3>Endereço</h3>
          <div class="grid">
            <FormField
              id="patient-cep"
              label="CEP"
              value={values.cep}
              oninput={(value) => (values.cep = value.replace(/\D/g, '').slice(0, 8))}
            />
            <FormField
              id="patient-street"
              label="Logradouro"
              value={values.logradouro}
              oninput={(value) => (values.logradouro = value)}
            />
            <FormField
              id="patient-number"
              label="Número"
              value={values.numero}
              oninput={(value) => (values.numero = value)}
            />
            <FormField
              id="patient-complement"
              label="Complemento"
              value={values.complemento}
              oninput={(value) => (values.complemento = value)}
            />
            <FormField
              id="patient-neighborhood"
              label="Bairro"
              value={values.bairro}
              oninput={(value) => (values.bairro = value)}
            />
            <FormField
              id="patient-city"
              label="Cidade"
              value={values.cidade}
              oninput={(value) => (values.cidade = value)}
            />
            <FormField
              id="patient-state"
              label="UF"
              value={values.estado}
              oninput={(value) => (values.estado = value.toUpperCase().slice(0, 2))}
            />
          </div>
        </section>
      {:else}
        <section>
          <h3>Responsável</h3>
          <div class="grid">
            <label
              >Responsável<select bind:value={values.responsavelId} required
                ><option value={null}>Selecione</option
                >{#each holders.filter((holder) => holder.status === 'ATIVO') as holder}<option
                    value={Number(holder.id.split(':')[1])}>{holder.nome}</option
                  >{/each}</select
              ></label
            >
            <label
              >Parentesco<select bind:value={values.parentesco} required
                ><option value="">Selecione</option><option>Filho(a)</option><option>Enteado(a)</option><option
                  >Tutelado(a)</option
                ><option>Outro</option></select
              ></label
            >
          </div>
        </section>
      {/if}
      {#if initial}<section>
          <h3>Situação cadastral</h3>
          <label
            >Situação<select bind:value={values.status}
              ><option value="ATIVO">Ativo</option><option value="INATIVO">Inativo</option></select
            ></label
          >
        </section>{/if}
      <footer>
        <Button variant="secondary" onclick={onCancel}>Cancelar</Button><Button type="submit">Salvar paciente</Button>
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
    width: min(100%, 44rem);
    max-height: calc(100dvh - 2rem);
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
    margin-bottom: var(--space-4);
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
  section {
    border-top: 1px solid var(--border-subtle);
    padding: var(--space-4) 0;
  }
  h3 {
    margin-bottom: var(--space-3);
    font-size: var(--text-base);
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
  .hint {
    align-self: end;
    color: var(--text-secondary);
    font-size: var(--text-xs);
  }
  footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);
    border-top: 1px solid var(--border-subtle);
    padding-top: var(--space-5);
  }
  @media (max-width: 600px) {
    .grid {
      grid-template-columns: 1fr;
    }
  }
</style>
