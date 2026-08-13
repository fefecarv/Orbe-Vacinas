<script lang="ts">
  import Alert from '../../design-system/components/Alert.svelte';
  import Button from '../../design-system/components/Button.svelte';
  import FormField from '../../design-system/components/FormField.svelte';
  import { authApi, roleFromUser } from '../../lib/api';
  let {
    onLogin,
    onNavigate,
  }: { onLogin: (role: 'patient' | 'employee' | 'admin') => void; onNavigate: (page: 'register' | 'forgot') => void } =
    $props();
  let email = $state('');
  let password = $state('');
  let error = $state('');
  let loading = $state(false);
  async function submit(event: SubmitEvent) {
    event.preventDefault();
    error = '';
    if (!email.includes('@') || password.length < 8) {
      error = 'Confira o e-mail e informe uma senha com pelo menos 8 caracteres.';
      return;
    }
    loading = true;
    try {
      const user = await authApi.login(email, password);
      onLogin(roleFromUser(user));
    } catch (exception) {
      error = exception instanceof Error ? exception.message : 'Não foi possível entrar.';
    } finally {
      loading = false;
    }
  }
</script>

<div class="heading">
  <p>Bem-vinda de volta</p>
  <h2>Entre na sua conta</h2>
  <span>Acesse seus agendamentos e sua carteira de vacinação.</span>
</div>
<form onsubmit={submit} novalidate>
  {#if error}<Alert tone="danger">{error}</Alert>{/if}<FormField
    id="email"
    label="E-mail"
    type="email"
    autocomplete="email"
    placeholder="nome@exemplo.com"
    value={email}
    oninput={(value) => (email = value)}
    required
  /><FormField
    id="password"
    label="Senha"
    type="password"
    autocomplete="current-password"
    placeholder="Digite sua senha"
    value={password}
    oninput={(value) => (password = value)}
    required
  /><button class="forgot" type="button" onclick={() => onNavigate('forgot')}>Esqueci minha senha</button><Button
    type="submit"
    fullWidth
    disabled={loading}>{loading ? 'Entrando...' : 'Entrar'}</Button
  >
</form>
<p class="footer">Ainda não tem uma conta? <button onclick={() => onNavigate('register')}>Cadastre-se</button></p>

<style>
  .heading > p {
    color: var(--color-brand-500);
    font-size: var(--text-xs);
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }
  .heading h2 {
    margin-top: var(--space-3);
    font-size: var(--text-3xl);
    letter-spacing: -0.04em;
  }
  .heading span {
    display: block;
    margin-top: var(--space-3);
    color: var(--text-secondary);
    line-height: 1.55;
  }
  form {
    display: grid;
    gap: var(--space-5);
    margin-top: var(--space-6);
  }
  .forgot {
    justify-self: end;
    border: 0;
    background: transparent;
    color: var(--color-brand-500);
    font-size: var(--text-sm);
    font-weight: 700;
    cursor: pointer;
  }
  .footer {
    margin-top: var(--space-6);
    color: var(--text-secondary);
    font-size: var(--text-sm);
    text-align: center;
  }
  .footer button {
    border: 0;
    background: transparent;
    color: var(--color-brand-500);
    font-weight: 750;
    cursor: pointer;
  }
</style>
