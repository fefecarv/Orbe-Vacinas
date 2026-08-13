<script lang="ts">
  import { untrack } from 'svelte';
  import Button from '../../design-system/components/Button.svelte';
  import FormField from '../../design-system/components/FormField.svelte';

  type Entity = 'user' | 'vaccine' | 'batch' | 'insurance';
  let {
    entity,
    initial = {},
    onSave,
    onCancel,
  }: {
    entity: Entity;
    initial?: Record<string, string>;
    onSave: (values: Record<string, string>) => void;
    onCancel: () => void;
  } = $props();

  const configs = {
    vaccine: {
      title: 'Vacina',
      fields: [
        ['name', 'Nome da vacina', 'text'],
        ['manufacturer', 'Fabricante', 'text'],
        ['description', 'Descrição', 'text'],
        ['category', 'Categoria', 'text'],
        ['age', 'Indicação', 'text'],
        ['doses', 'Esquema de doses', 'text'],
        ['price', 'Valor-base', 'text'],
        ['status', 'Situação', 'status'],
      ],
    },
    batch: {
      title: 'Lote',
      fields: [
        ['number', 'Número do lote', 'text'],
        ['vaccine', 'Vacina', 'text'],
        ['expires', 'Validade', 'text'],
        ['quantity', 'Quantidade', 'text'],
        ['supplier', 'Fornecedor', 'text'],
        ['status', 'Situação', 'batchStatus'],
      ],
    },
    insurance: {
      title: 'Convênio',
      fields: [
        ['company', 'Convênio', 'text'],
        ['plan', 'Plano', 'text'],
        ['code', 'Código operacional', 'text'],
        ['coverageType', 'Tipo de cobertura', 'coverageType'],
        ['discount', 'Percentual de desconto', 'optional'],
        ['copay', 'Valor da coparticipação', 'optional'],
        ['status', 'Situação', 'status'],
      ],
    },
  } as const;

  let values = $state<Record<string, string>>(
    untrack(() => ({
      role: 'Paciente',
      status: 'Ativo',
      unit: 'Unidade Centro',
      requirePasswordChange: 'Sim',
      ...initial,
    })),
  );
  let error = $state('');
  let isUser = $derived(entity === 'user');
  let isStaff = $derived(values.role === 'Funcionário' || values.role === 'Administrador');
  let editing = $derived(Object.keys(initial).length > 0);

  function submit(event: SubmitEvent) {
    event.preventDefault();
    const required = isUser
      ? ['name', 'cpf', 'email', 'phone', 'birth', 'role', 'status', ...(editing ? [] : ['password'])]
      : configs[entity as Exclude<Entity, 'user'>].fields
          .filter((field) => !['status', 'discount', 'copay'].includes(field[0]))
          .map((field) => field[0]);
    if (required.some((field) => !values[field]?.trim())) {
      error = 'Preencha todos os campos obrigatórios.';
      return;
    }
    if (isUser && !values.email.includes('@')) {
      error = 'Informe um endereço de e-mail válido.';
      return;
    }
    onSave({ ...values, lastAccess: values.lastAccess || 'Ainda não acessou' });
  }
</script>

<div class="backdrop" role="presentation" onclick={(event) => event.target === event.currentTarget && onCancel()}>
  <div role="dialog" aria-modal="true" aria-labelledby="crud-title">
    <form onsubmit={submit}>
      <header>
        <div>
          <p>{editing ? 'Editar registro' : 'Novo cadastro'}</p>
          <h2 id="crud-title">{isUser ? 'Identidade e acesso' : configs[entity as Exclude<Entity, 'user'>].title}</h2>
        </div>
        <button type="button" aria-label="Fechar" onclick={onCancel}>×</button>
      </header>

      {#if error}<div class="error" role="alert">{error}</div>{/if}

      {#if isUser}
        <section>
          <div class="section-heading">
            <span>1</span>
            <div>
              <h3>Dados pessoais</h3>
              <p>Identificação principal da pessoa no sistema.</p>
            </div>
          </div>
          <div class="fields">
            <FormField
              id="user-name"
              label="Nome completo"
              value={values.name ?? ''}
              oninput={(v) => (values.name = v)}
              required
            />
            <FormField
              id="user-cpf"
              label="CPF"
              placeholder="000.000.000-00"
              value={values.cpf ?? ''}
              oninput={(v) => (values.cpf = v)}
              required
            />
            <FormField
              id="user-email"
              label="E-mail"
              type="email"
              value={values.email ?? ''}
              oninput={(v) => (values.email = v)}
              required
            />
            <FormField
              id="user-phone"
              label="Telefone"
              placeholder="(00) 00000-0000"
              value={values.phone ?? ''}
              oninput={(v) => (values.phone = v)}
              required
            />
            <FormField id="user-birth" label="Data de nascimento" type="date" value={values.birth ?? ''} oninput={(v)=>(values.birth=v)} required />
          </div>
        </section>

        <section>
          <div class="section-heading">
            <span>2</span>
            <div>
              <h3>Perfil de acesso</h3>
              <p>Define o que essa pessoa poderá consultar e administrar.</p>
            </div>
          </div>
          <div class="fields">
            <label
              >Perfil
              <select value={values.role} onchange={(event) => (values.role = event.currentTarget.value)}>
                <option>Paciente</option>
                <option>Funcionário</option>
                <option>Administrador</option>
              </select>
            </label>
            <label
              >Situação
              <select value={values.status} onchange={(event) => (values.status = event.currentTarget.value)}>
                <option>Ativo</option><option>Inativo</option><option>Bloqueado</option>
              </select>
            </label>
            {#if isStaff}
              <label
                >Unidade
                <select value={values.unit} onchange={(event) => (values.unit = event.currentTarget.value)}>
                  <option>Unidade Centro</option><option>Unidade Norte</option><option>Todas as unidades</option>
                </select>
              </label>
              <FormField
                id="user-registration"
                label={values.role === 'Administrador' ? 'Código interno' : 'Matrícula profissional'}
                value={values.registration ?? ''}
                oninput={(v) => (values.registration = v)}
              />
            {/if}
          </div>
          <div class="access-summary">
            <b>{values.role}</b>
            <span
              >{values.role === 'Administrador'
                ? 'Acesso total a usuários, vacinas, estoque, convênios, relatórios e auditoria.'
                : values.role === 'Funcionário'
                  ? 'Acesso à agenda, pacientes e registro de aplicações.'
                  : 'Acesso somente ao próprio portal, dependentes e carteira vacinal.'}</span
            >
          </div>
        </section>

        <section>
          <div class="section-heading">
            <span>3</span>
            <div>
              <h3>Segurança inicial</h3>
              <p>Configure como o primeiro acesso será realizado.</p>
            </div>
          </div>
          <div class="fields">
            <FormField
              id="user-password"
              label={editing ? 'Nova senha temporária (opcional)' : 'Senha temporária'}
              type="password"
              value={values.password ?? ''}
              oninput={(v) => (values.password = v)}
            />
            <label
              >Troca de senha no primeiro acesso
              <select
                value={values.requirePasswordChange}
                onchange={(event) => (values.requirePasswordChange = event.currentTarget.value)}
              >
                <option>Sim</option><option>Não</option>
              </select>
            </label>
          </div>
        </section>
      {:else}
        <div class="fields">
          {#each configs[entity as Exclude<Entity, 'user'>].fields as field}
            {#if field[2] === 'status'}
              <label
                >{field[1]}<select
                  value={values[field[0]] ?? 'Ativo'}
                  onchange={(e) => (values[field[0]] = e.currentTarget.value)}
                  ><option>Ativo</option><option>Inativo</option></select
                ></label
              >
            {:else if field[2] === 'batchStatus'}
              <label
                >{field[1]}<select
                  value={values[field[0]] ?? 'Regular'}
                  onchange={(e) => (values[field[0]] = e.currentTarget.value)}
                  ><option>Regular</option><option>Atenção</option><option>Crítico</option><option>Inativo</option
                  ></select
                ></label
              >
            {:else if field[2] === 'coverageType'}
              <label>{field[1]}<select value={values[field[0]] ?? 'ANALISE_MANUAL'} onchange={(e) => (values[field[0]] = e.currentTarget.value)}><option value="INTEGRAL">Cobertura integral</option><option value="PERCENTUAL">Desconto percentual</option><option value="COPARTICIPACAO">Coparticipação fixa</option><option value="SEM_COBERTURA">Sem cobertura</option><option value="ANALISE_MANUAL">Análise manual</option></select></label>
            {:else}
              <FormField
                id={`crud-${field[0]}`}
                label={field[1]}
                value={values[field[0]] ?? ''}
                oninput={(v) => (values[field[0]] = v)}
                required={field[2] !== 'optional'}
              />
            {/if}
          {/each}
        </div>
      {/if}

      <footer>
        <Button variant="secondary" onclick={onCancel}>Cancelar</Button>
        <Button type="submit">{editing ? 'Salvar alterações' : isUser ? 'Criar acesso' : 'Cadastrar'}</Button>
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
    padding: var(--space-4);
  }
  form {
    width: min(100%, 48rem);
    max-height: calc(100dvh - 2rem);
    overflow: auto;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-lg);
    background: var(--surface-card);
    padding: var(--space-5);
    box-shadow: var(--shadow-md);
  }
  header {
    display: flex;
    align-items: flex-start;
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
    margin-top: 0.15rem;
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
  .section-heading {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    margin-bottom: var(--space-3);
  }
  .section-heading > span {
    display: grid;
    width: 1.75rem;
    height: 1.75rem;
    place-items: center;
    border-radius: 50%;
    background: var(--color-brand-50);
    color: var(--color-brand-700);
    font-size: var(--text-xs);
    font-weight: 800;
  }
  .section-heading h3 {
    font-size: var(--text-md);
  }
  .section-heading p {
    margin-top: 0.1rem;
    color: var(--text-secondary);
    font-size: var(--text-xs);
  }
  .fields {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-3) var(--space-4);
  }
  label {
    display: grid;
    gap: var(--space-2);
    color: var(--text-primary);
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
  .access-summary {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    margin-top: var(--space-3);
    border-radius: var(--radius-md);
    background: var(--surface-subtle);
    padding: var(--space-3);
    font-size: var(--text-xs);
  }
  .access-summary b {
    color: var(--color-brand-600);
    white-space: nowrap;
  }
  .access-summary span {
    color: var(--text-secondary);
    line-height: 1.45;
  }
  .error {
    margin-bottom: var(--space-3);
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
    border-top: 1px solid var(--border-subtle);
    padding-top: var(--space-4);
  }
  @media (max-width: 600px) {
    .fields {
      grid-template-columns: 1fr;
    }
    form {
      padding: var(--space-4);
    }
  }
</style>
