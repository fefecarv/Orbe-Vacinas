<script lang="ts">
  import Alert from '../../design-system/components/Alert.svelte';
  import Button from '../../design-system/components/Button.svelte';
  import Card from '../../design-system/components/Card.svelte';
  import FormField from '../../design-system/components/FormField.svelte';
  import PageHeader from '../../design-system/components/PageHeader.svelte';
  import Toast from '../../design-system/components/Toast.svelte';
  import {authApi} from '../../lib/api';

  let { mode }: { mode: 'security' | 'help' } = $props();
  let toast = $state('');
  let twoFactor = $state(false);
  let question = $state('');
  let answer = $state('');
  let currentPassword=$state(''),newPassword=$state('');
  const faqs = [
    [
      'Como reagendar uma vacina?',
      'Abra Agendamentos, escolha o atendimento e selecione Reagendar. A data anterior fica registrada no histórico.',
    ],
    [
      'Posso agendar para meus filhos?',
      'Sim. Na primeira etapa do agendamento, selecione o dependente que será vacinado.',
    ],
    ['Onde encontro os comprovantes?', 'Acesse Carteira vacinal, selecione a pessoa e abra os detalhes da aplicação.'],
    [
      'O que levar no atendimento?',
      'Documento com foto, carteira de vacinação e carteirinha do convênio, quando aplicável.',
    ],
  ];
</script>

<div class="page">
  {#if mode === 'security'}
    <PageHeader
      eyebrow="Minha conta"
      title="Segurança"
      description="Proteja sua conta e acompanhe os acessos recentes."
    />
    <div class="grid">
      <Card padding="lg">
        <h2>Alterar senha</h2>
        <p>Use uma senha exclusiva com pelo menos oito caracteres.</p>
        <form
          onsubmit={async (e) => {
            e.preventDefault();
            try{await authApi.changePassword(currentPassword,newPassword);toast='Senha atualizada com sucesso.';currentPassword='';newPassword='';}catch(exception){toast=exception instanceof Error?exception.message:'Não foi possível alterar a senha.';}
          }}
        >
          <FormField id="current-password" label="Senha atual" type="password" value={currentPassword} oninput={v=>currentPassword=v}/>
          <FormField id="new-security-password" label="Nova senha" type="password" hint="Mínimo de 8 caracteres." value={newPassword} oninput={v=>newPassword=v}/>
          <Button type="submit">Atualizar senha</Button>
        </form>
      </Card>
      <Card padding="lg">
        <h2>Verificação em duas etapas</h2>
        <p>Adicione uma confirmação extra ao entrar na sua conta.</p>
        <label class="switch"
          ><span><strong>Confirmação por e-mail</strong><small>{twoFactor ? 'Ativada' : 'Desativada'}</small></span
          ><input
            type="checkbox"
            bind:checked={twoFactor}
            onchange={() => (toast = twoFactor ? 'Verificação ativada.' : 'Verificação desativada.')}
          /></label
        >
      </Card>
      <Card padding="lg">
        <h2>Sessões ativas</h2>
        <p>Dispositivos que acessaram sua conta recentemente.</p>
        <div class="session">
          <span>▣</span>
          <div><strong>Chrome · Windows</strong><small>Salvador, BA · Sessão atual</small></div>
          <b>Agora</b>
        </div>
        <div class="session">
          <span>□</span>
          <div><strong>Safari · iPhone</strong><small>Salvador, BA</small></div>
          <b>Ontem</b>
        </div>
        <Button variant="secondary" size="sm" onclick={() => (toast = 'As outras sessões foram encerradas.')}
          >Encerrar outras sessões</Button
        >
      </Card>
    </div>
  {:else}
    <PageHeader
      eyebrow="Suporte"
      title="Central de ajuda"
      description="Encontre orientações ou fale com nossa equipe."
    />
    <div class="help-grid">
      <section>
        <h2>Perguntas frequentes</h2>
        {#each faqs as faq}
          <button class:open={question === faq[0]} onclick={() => (question = question === faq[0] ? '' : faq[0])}>
            <span
              ><strong>{faq[0]}</strong>{#if question === faq[0]}<small>{faq[1]}</small>{/if}</span
            ><b>{question === faq[0] ? '−' : '+'}</b>
          </button>
        {/each}
      </section>
      <Card padding="lg">
        <h2>Fale com a clínica</h2>
        <p>Atendimento de segunda a sexta, das 8h às 18h.</p>
        <div class="contact">
          <span>☎</span>
          <div><small>Telefone e WhatsApp</small><strong>(71) 3333-2026</strong></div>
        </div>
        <div class="contact">
          <span>@</span>
          <div><small>E-mail</small><strong>atendimento@orbe.com</strong></div>
        </div>
        <form
          onsubmit={(e) => {
            e.preventDefault();
            toast = 'Solicitação enviada. Responderemos em breve.';
            answer = '';
          }}
        >
          <label>Como podemos ajudar?<textarea bind:value={answer} placeholder="Descreva sua dúvida"></textarea></label>
          <Button type="submit" disabled={!answer.trim()}>Enviar solicitação</Button>
        </form>
      </Card>
    </div>
    <Alert>Em caso de reação grave após a vacinação, procure atendimento médico de urgência.</Alert>
  {/if}
</div>
{#if toast}<Toast message={toast} onClose={() => (toast = '')} />{/if}

<style>
  .page {
    width: min(100%, var(--content-max));
    margin: 0 auto;
    padding: var(--space-8);
  }
  .grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-4);
    margin-top: var(--space-6);
  }
  h2 {
    font-size: var(--text-lg);
  }
  p {
    margin-top: var(--space-2);
    color: var(--text-secondary);
    font-size: var(--text-sm);
    line-height: 1.55;
  }
  form {
    display: grid;
    gap: var(--space-4);
    margin-top: var(--space-5);
  }
  .switch {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: var(--space-6);
  }
  .switch span {
    display: grid;
    gap: 0.25rem;
  }
  .switch small {
    color: var(--text-secondary);
  }
  .switch input {
    width: 2.6rem;
    height: 1.4rem;
    accent-color: var(--color-brand-500);
  }
  .session,
  .contact {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    border-top: 1px solid var(--border-subtle);
    padding: var(--space-4) 0;
  }
  .session > span,
  .contact > span {
    display: grid;
    width: 2.2rem;
    height: 2.2rem;
    place-items: center;
    border-radius: 50%;
    background: var(--surface-subtle);
    color: var(--color-brand-500);
  }
  .session > div,
  .contact > div {
    display: grid;
    flex: 1;
    gap: 0.2rem;
  }
  .session small,
  .contact small {
    color: var(--text-secondary);
  }
  .session > b {
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
  .help-grid {
    display: grid;
    grid-template-columns: 1.25fr 0.75fr;
    gap: var(--space-5);
    margin: var(--space-6) 0;
  }
  .help-grid section > h2 {
    margin-bottom: var(--space-4);
  }
  .help-grid section > button {
    display: flex;
    width: 100%;
    align-items: flex-start;
    justify-content: space-between;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: var(--space-4);
    color: var(--text-primary);
    text-align: left;
    cursor: pointer;
    margin-bottom: var(--space-3);
  }
  .help-grid section button span {
    display: grid;
    gap: var(--space-3);
  }
  .help-grid section button small {
    max-width: 38rem;
    color: var(--text-secondary);
    font-weight: 400;
    line-height: 1.55;
  }
  .help-grid section button.open {
    border-color: var(--color-brand-500);
  }
  textarea {
    min-height: 7rem;
    resize: vertical;
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: var(--space-3);
    color: var(--text-primary);
  }
  form > label {
    display: grid;
    gap: var(--space-2);
    font-size: var(--text-sm);
    font-weight: 650;
  }
  @media (max-width: 850px) {
    .grid,
    .help-grid {
      grid-template-columns: 1fr;
    }
  }
  @media (max-width: 680px) {
    .page {
      padding: var(--space-5);
    }
  }
</style>
