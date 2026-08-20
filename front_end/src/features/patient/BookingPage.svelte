<script lang="ts">
  import { onMount } from 'svelte';
  import Alert from '../../design-system/components/Alert.svelte';
  import Button from '../../design-system/components/Button.svelte';
  import Card from '../../design-system/components/Card.svelte';
  import PageHeader from '../../design-system/components/PageHeader.svelte';
  import Tooltip from '../../design-system/components/Tooltip.svelte';
  import { currentUser, patientApi, type InsuranceAnalysis } from '../../lib/api';
  let {
    initialVaccine = '',
    onFinish,
    onCancel,
  }: { initialVaccine?: string; onFinish: () => void; onCancel: () => void } = $props();
  const user = currentUser();
  const currentPatient = { id: String(user?.id ?? ''), name: user?.nome ?? 'Titular', firstName: user?.nome.split(' ')[0] ?? 'Paciente' };
  type VaccineOption = { id:string; name:string; manufacturer:string; doses:string; price:number; available:boolean;minAge:number;maxAge:number|null };
  type DependentOption = { id:string; name:string; relationship:string; age:string;birthDate:string };
  type InsuranceOption = { id:string; company:string; plan:string; cardNumber:string; active:boolean };
  let vaccines = $state<VaccineOption[]>([]);
  let dependents = $state<DependentOption[]>([]);
  let insurances = $state<InsuranceOption[]>([]);
  let step = $state(1);
  let patient = $state(currentPatient.id);
  let vaccine = $state('');
  let date = $state(new Date(Date.now() + 86_400_000).toISOString().slice(0, 10));
  let time = $state('09:30');
  let availableTimes = $state<string[]>([]);
  let insurance = $state('private');
  let done = $state(false);
  let loading = $state(true);
  let submitting = $state(false);
  let error = $state('');
  let protocol = $state('');
  let analysis = $state<InsuranceAnalysis | null>(null);
  let analyzing = $state(false);
  const steps = ['Paciente e vacina', 'Data e horário', 'Pagamento', 'Revisão'];
  let selectedVaccine = $derived(vaccines.find((v) => v.id === vaccine));
  function eligibility(item:VaccineOption){const dependent=dependents.find(person=>person.id===patient);if(!dependent)return {ok:true,message:''};const birth=new Date(`${dependent.birthDate}T12:00:00`),now=new Date();const months=(now.getFullYear()-birth.getFullYear())*12+now.getMonth()-birth.getMonth();const ok=months>=item.minAge&&(item.maxAge===null||months<=item.maxAge);return {ok,message:ok?'':`Indicada a partir de ${item.minAge} meses${item.maxAge===null?'':` até ${item.maxAge} meses`}`};}
  let canContinue = $derived(
    step === 1 ? !!patient && !!vaccine : step === 2 ? !!date && !!time : step === 3 ? !!insurance : true,
  );
  async function loadTimes(){time='';if(!date)return;try{availableTimes=await patientApi.availableTimes(date);if(availableTimes.length)time=availableTimes[0].slice(0,5);}catch(exception){availableTimes=[];error=exception instanceof Error?exception.message:'Não foi possível consultar os horários.';}}
  $effect(() => {
    if (!vaccine && initialVaccine) vaccine = initialVaccine;
  });
  async function next() {
    if (step === 3 && selectedVaccine) {
      await analyzeCoverage();
      if (!analysis) return;
    }
    if (step < 4) {
      step += 1;
      return;
    }
    if (!selectedVaccine) return;
    submitting = true; error = '';
    try {
      const isHolder = patient === currentPatient.id;
      const result = await patientApi.createAppointment({
        usuarioId: isHolder ? Number(currentPatient.id) : null,
        dependenteId: isHolder ? null : Number(patient),
        vacinaId: Number(vaccine),
        convenioId: insurance === 'private' ? null : Number(insurance),
        dataAgendamento: `${date}T${time}:00`, unidade: 'Orbe Centro', sala: 'A confirmar',
        dosePrevista: selectedVaccine.doses,
        tipoAtendimento: insurance === 'private' ? 'PARTICULAR' : 'CONVENIO',
        valorEstimado: null,
      });
      protocol = result.protocolo; done = true;
    } catch (exception) {
      error = exception instanceof Error ? exception.message : 'Não foi possível criar o agendamento.';
    } finally { submitting = false; }
  }
  async function analyzeCoverage() {
    if (!selectedVaccine) return;
    analyzing = true; error = '';
    try {
      analysis = await patientApi.analyzeInsurance(Number(selectedVaccine.id), insurance === 'private' ? undefined : Number(insurance));
    } catch (exception) {
      analysis = null;
      error = exception instanceof Error ? exception.message : 'Não foi possível analisar o convênio.';
    } finally { analyzing = false; }
  }
  onMount(async () => {
    try {
      const [apiVaccines, apiDependents, apiInsurances] = await Promise.all([
        patientApi.vaccines(), patientApi.dependents(), patientApi.insurances(),
      ]);
      vaccines = apiVaccines.map((item) => ({ id:String(item.id), name:item.nome, manufacturer:item.fabricante, doses:item.esquemaDoses, price:item.valorBase, available:item.ativo,minAge:item.idadeMinimaMeses??0,maxAge:item.idadeMaximaMeses??null }));
      dependents = apiDependents.map((item) => ({ id:String(item.id), name:item.nome, relationship:'Dependente', age:new Date(item.dataNascimento).toLocaleDateString('pt-BR'),birthDate:item.dataNascimento }));
      insurances = apiInsurances.map((item) => ({ id:String(item.id), company:item.nomeConvenio, plan:item.plano, cardNumber:item.numeroCarteirinha, active:item.ativo }));await loadTimes();
    } catch (exception) { error = exception instanceof Error ? exception.message : 'Não foi possível carregar os dados.'; }
    finally { loading = false; }
  });
  function back() {
    if (step > 1) step -= 1;
    else onCancel();
  }
</script>

<div class="page">
  <PageHeader
    eyebrow="Agendamento"
    title={done ? 'Agendamento confirmado' : 'Agendar vacina'}
    description={done ? 'Seu horário foi reservado com sucesso.' : 'Conclua as etapas para reservar seu atendimento.'}
  />
  {#if done}<div class="confirmation">
      <div class="check">✓</div>
      <h2>Tudo certo, {currentPatient.firstName}!</h2>
      <p>Enviamos a confirmação para o e-mail cadastrado.</p>
      <Card
        ><dl>
          <div>
            <dt>Protocolo</dt>
            <dd>{protocol}</dd>
          </div>
          <div>
            <dt>Vacina</dt>
            <dd>{selectedVaccine?.name}</dd>
          </div>
          <div>
            <dt>Data e horário</dt>
            <dd>{new Date(`${date}T${time}:00`).toLocaleDateString('pt-BR')} · {time}</dd>
          </div>
          <div>
            <dt>Paciente</dt>
            <dd>
              {patient === currentPatient.id ? currentPatient.name : dependents.find((d) => d.id === patient)?.name}
            </dd>
          </div>
        </dl></Card
      >
      <div class="confirm-actions">
        <Button onclick={onFinish}>Ver meus agendamentos</Button><Button
          variant="secondary"
          onclick={() => window.print()}>Imprimir comprovante</Button
        >
      </div>
    </div>
  {:else}<ol class="steps">
      {#each steps as label, index}<li class:active={step === index + 1} class:complete={step > index + 1}>
          <span>{step > index + 1 ? '✓' : index + 1}</span>
          <p>{label}</p>
        </li>{/each}
    </ol>
    <Card padding="lg">
      {#if error}<Alert tone="danger">{error}</Alert>{/if}
      {#if loading}<p>Carregando opções de agendamento...</p>{/if}
      {#if step === 1}<div class="section-title">
          <h2>Quem será vacinado?</h2>
          <Tooltip text="Selecione o titular ou um dependente." />
        </div>
        <div class="options">
          <label class:selected={patient === currentPatient.id}
            ><input type="radio" bind:group={patient} value={currentPatient.id} /><span class="option-icon">F</span
            ><strong>{currentPatient.name}<small>Titular</small></strong></label
          >{#each dependents as person}<label class:selected={patient === person.id}
              ><input type="radio" bind:group={patient} value={person.id} /><span class="option-icon"
                >{person.name.slice(0, 1)}</span
              ><strong>{person.name}<small>{person.relationship} · {person.age}</small></strong></label
            >{/each}
        </div>
        <div class="section-title second">
          <h2>Escolha a vacina</h2>
          <Tooltip text="São exibidas somente vacinas disponíveis." />
        </div>
        <div class="vaccine-options">
          {#each vaccines.filter((v) => v.available) as item}{@const indication=eligibility(item)}<label class:selected={vaccine === item.id} class:unavailable={!indication.ok}
              ><input type="radio" bind:group={vaccine} value={item.id} disabled={!indication.ok}/><span
                ><strong>{item.name}</strong><small>{indication.ok?`${item.manufacturer} · ${item.doses}`:indication.message}</small></span
              ><b>{item.price.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}</b></label
            >{/each}
        </div>
      {:else if step === 2}<div class="section-title">
          <h2>Selecione a data e o horário</h2>
          <Tooltip text="Horários disponíveis na Unidade Centro." />
        </div>
        <label class="date-label">Data<input type="date" bind:value={date} min={new Date().toISOString().slice(0, 10)} onchange={loadTimes} /></label>
        <div class="times">
          {#each availableTimes as raw}{@const item=raw.slice(0,5)}<button
              class:selected={time === item}
              onclick={() => (time = item)}>{item}</button>{/each}
        </div>
        {#if availableTimes.length===0}<Alert>A clínica não possui horários disponíveis nesta data. Escolha outro dia.</Alert>{/if}
        <Alert>Chegue com 10 minutos de antecedência e leve um documento com foto.</Alert>
      {:else if step === 3}<div class="section-title">
          <h2>Como será o atendimento?</h2>
          <Tooltip text="Escolha entre particular ou um convênio cadastrado." />
        </div>
        <div class="payment-options">
          <label class:selected={insurance === 'private'}
            ><input type="radio" bind:group={insurance} value="private" /><span
              ><strong>Particular</strong><small>Pagamento realizado na clínica</small></span
            ><b>{selectedVaccine?.price.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}</b></label
          >{#each insurances.filter((i) => i.active) as item}<label class:selected={insurance === item.id}
              ><input type="radio" bind:group={insurance} value={item.id} /><span
                ><strong>{item.company}</strong><small>{item.plan} · Final {item.cardNumber.slice(-4)}</small></span
              ><b>Análise automática</b></label
            >{/each}
        </div>
      {:else}<div class="section-title">
          <h2>Revise seu agendamento</h2>
          <Tooltip text="Confirme se todos os dados estão corretos." />
        </div>
        <div class="review">
          <dl>
            <div>
              <dt>Paciente</dt>
              <dd>
                {patient === currentPatient.id ? currentPatient.name : dependents.find((d) => d.id === patient)?.name}
              </dd>
            </div>
            <div>
              <dt>Vacina</dt>
              <dd>{selectedVaccine?.name}</dd>
            </div>
            <div>
              <dt>Data e horário</dt>
              <dd>{new Date(`${date}T${time}:00`).toLocaleDateString('pt-BR')} · {time}</dd>
            </div>
            <div>
              <dt>Unidade</dt>
              <dd>Orbe Centro · Sala a confirmar</dd>
            </div>
            <div>
              <dt>Atendimento</dt>
              <dd>{insurance === 'private' ? 'Particular' : insurances.find((i) => i.id === insurance)?.company}</dd>
            </div>
            <div>
              <dt>Valor estimado</dt>
              <dd>
                {analyzing ? 'Analisando...' : analysis?.valorPaciente == null
                  ? analysis?.mensagem ?? 'A calcular'
                  : analysis.valorPaciente.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
              </dd>
            </div>
            {#if analysis && insurance !== 'private'}
              <div><dt>Valor da vacina</dt><dd>{analysis.valorBase.toLocaleString('pt-BR', {style:'currency',currency:'BRL'})}</dd></div>
              <div><dt>Cobertura do convênio</dt><dd>-{(analysis.valorCoberto ?? 0).toLocaleString('pt-BR', {style:'currency',currency:'BRL'})}</dd></div>
              <div><dt>Análise automática</dt><dd>{analysis.status} · {analysis.mensagem}</dd></div>
            {/if}
          </dl>
        </div>
        <Alert>Ao confirmar, você declara que os dados informados estão corretos.</Alert>{/if}
      <footer>
        <Button variant="secondary" onclick={back}>{step === 1 ? 'Cancelar' : 'Voltar'}</Button><Button
          onclick={next}
          disabled={!canContinue || loading || submitting}>{submitting ? 'Confirmando...' : step === 4 ? 'Confirmar agendamento' : 'Continuar'}</Button
        >
      </footer></Card
    >{/if}
</div>

<style>
  .page {
    width: min(100%, 65rem);
    margin: 0 auto;
    padding: var(--space-8);
  }
  .steps {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    margin: var(--space-6) 0;
    padding: 0;
    list-style: none;
  }
  .steps li {
    position: relative;
    display: grid;
    justify-items: center;
    gap: var(--space-2);
    color: var(--text-tertiary);
    font-size: var(--text-xs);
    text-align: center;
  }
  .steps li:not(:last-child)::after {
    position: absolute;
    z-index: -1;
    top: 1rem;
    left: 55%;
    width: 90%;
    height: 1px;
    background: var(--border-strong);
    content: '';
  }
  .steps span {
    display: grid;
    width: 2rem;
    height: 2rem;
    place-items: center;
    border: 1px solid var(--border-strong);
    border-radius: 50%;
    background: var(--surface-card);
    font-weight: 800;
  }
  .steps .active,
  .steps .complete {
    color: var(--color-brand-500);
  }
  .steps .active span,
  .steps .complete span {
    border-color: var(--color-brand-500);
    background: var(--color-brand-500);
    color: white;
  }
  .steps .complete:not(:last-child)::after {
    background: var(--color-brand-500);
  }
  .section-title {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    margin-bottom: var(--space-5);
  }
  .section-title.second {
    margin-top: var(--space-6);
  }
  .section-title h2 {
    font-size: var(--text-xl);
  }
  .options {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-3);
  }
  .options label,
  .vaccine-options label,
  .payment-options label {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-4);
    cursor: pointer;
  }
  .options label.selected,
  .vaccine-options label.selected,
  .payment-options label.selected {
    border-color: var(--color-brand-500);
    box-shadow: 0 0 0 2px var(--focus-ring);
  }
  .vaccine-options label.unavailable{opacity:.55;cursor:not-allowed}.vaccine-options label.unavailable small{color:var(--status-danger)}
  label input[type='radio'] {
    position: absolute;
    opacity: 0;
  }
  .option-icon {
    display: grid;
    width: 2.4rem;
    height: 2.4rem;
    place-items: center;
    border-radius: 50%;
    background: var(--surface-subtle);
    color: var(--color-brand-500);
    font-weight: 800;
  }
  .options strong,
  .vaccine-options strong,
  .payment-options strong {
    font-size: var(--text-sm);
  }
  .options small,
  .vaccine-options small,
  .payment-options small {
    display: block;
    margin-top: 0.2rem;
    color: var(--text-secondary);
    font-weight: 400;
  }
  .vaccine-options,
  .payment-options {
    display: grid;
    gap: var(--space-3);
  }
  .vaccine-options label > span,
  .payment-options label > span {
    flex: 1;
  }
  .vaccine-options b,
  .payment-options b {
    font-size: var(--text-sm);
  }
  .date-label {
    display: grid;
    max-width: 20rem;
    gap: var(--space-2);
    font-size: var(--text-sm);
    font-weight: 650;
  }
  .date-label input {
    min-height: 2.75rem;
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: 0 var(--space-4);
    color: var(--text-primary);
  }
  .times {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-3);
    margin: var(--space-6) 0;
  }
  .times button {
    min-height: 2.7rem;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    color: var(--text-primary);
    cursor: pointer;
  }
  .times button.selected {
    border-color: var(--color-brand-500);
    background: var(--color-brand-50);
    color: var(--color-brand-600);
    font-weight: 800;
  }
  :global([data-theme='dark']) .times button.selected {
    background: rgb(23 71 255/0.16);
    color: #9db0ff;
  }
  .review {
    margin-bottom: var(--space-5);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-5);
  }
  dl {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
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
  footer {
    display: flex;
    justify-content: space-between;
    margin-top: var(--space-6);
    border-top: 1px solid var(--border-subtle);
    padding-top: var(--space-5);
  }
  .confirmation {
    display: grid;
    max-width: 38rem;
    justify-items: center;
    margin: var(--space-12) auto;
    text-align: center;
  }
  .check {
    display: grid;
    width: 4rem;
    height: 4rem;
    place-items: center;
    border-radius: 50%;
    background: var(--status-success-bg);
    color: var(--status-success);
    font-size: 1.8rem;
    font-weight: 800;
  }
  .confirmation h2 {
    margin-top: var(--space-5);
    font-size: var(--text-2xl);
  }
  .confirmation > p {
    margin: var(--space-2) 0 var(--space-6);
    color: var(--text-secondary);
  }
  .confirmation :global(.card) {
    width: 100%;
    text-align: left;
  }
  .confirm-actions {
    display: flex;
    gap: var(--space-3);
    margin-top: var(--space-6);
  }
  @media (max-width: 680px) {
    .page {
      padding: var(--space-5);
    }
    .steps p {
      display: none;
    }
    .options {
      grid-template-columns: 1fr;
    }
    .times {
      grid-template-columns: repeat(3, 1fr);
    }
    dl {
      grid-template-columns: 1fr;
    }
    .vaccine-options b,
    .payment-options b {
      display: none;
    }
    .confirm-actions {
      align-items: stretch;
      flex-direction: column;
    }
  }
</style>
