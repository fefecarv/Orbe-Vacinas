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
  import PatientDialog from './PatientDialog.svelte';
  import type { QueueStatus } from '../../mocks/employee';
  import { currentUser, staffApi, type ApiBatch, type RegisteredApplication, type StaffPatient, type StaffPatientInput } from '../../lib/api';
  let {
    mode,
    onNavigate,
  }: { mode: 'dashboard' | 'agenda' | 'patients' | 'application'; onNavigate: (page: string) => void } = $props();
  type DailyAppointment = { id:string; time:string; patient:string; cpf:string; vaccine:string; vaccineId:number; usuarioId:number|null; dependenteId:number|null; dose:string; room:string; status:QueueStatus; tipoAtendimento:'PARTICULAR'|'CONVENIO'|'CAMPANHA' };
  let dailyAppointments = $state<DailyAppointment[]>([]);
  let statuses = $state<Record<string, QueueStatus>>({});
  let patients = $state<StaffPatient[]>([]);
  let query = $state('');
  let applied = $state(false);
  let selectedPatient = $state('');
  let selectedVaccine = $state('');
  let batch = $state('');
  let dose = $state('');
  let receipt = $state<RegisteredApplication | null>(null);
  let availableBatches = $state<ApiBatch[]>([]);
  let applicationDate = $state(new Date().toISOString().slice(0, 10));
  let applicationTime = $state(new Date().toTimeString().slice(0, 5));
  let administrationRoute = $state('Intramuscular');
  let applicationSite = $state('Deltoide direito');
  let applicationError = $state('');
  let submittingApplication = $state(false);
  let patientDialog = $state(false);
  let editingPatient = $state<StaffPatient | null>(null);
  let toast = $state('');
  let agendaLoading = $state(true);
  let agendaError = $state('');
  const staffUser = currentUser();
  const today = new Date().toISOString().slice(0, 10);
  let agendaView = $state<'grid' | 'list'>(
    (localStorage.getItem('orbe-view-staff-agenda') as 'grid' | 'list') ?? 'list',
  );
  let patientView = $state<'grid' | 'list'>(
    (localStorage.getItem('orbe-view-staff-patients') as 'grid' | 'list') ?? 'list',
  );
  $effect(() => localStorage.setItem('orbe-view-staff-agenda', agendaView));
  $effect(() => localStorage.setItem('orbe-view-staff-patients', patientView));
  const labels: Record<QueueStatus, string> = {
    confirmed: 'Confirmado',
    waiting: 'Na espera',
    in_service: 'Em atendimento',
    completed: 'Concluído',
  };
  let filteredPatients = $derived(
    patients.filter((p) => p.nome.toLowerCase().includes(query.toLowerCase()) || p.cpf.includes(query)),
  );
  let selectedClinicalAppointment = $derived(dailyAppointments.find((item) => item.id === selectedPatient));
  let selectedBatch = $derived(availableBatches.find((item) => String(item.id) === batch));
  async function advance(id: string) {
    const order: QueueStatus[] = ['confirmed', 'waiting', 'in_service', 'completed'];
    const current = order.indexOf(statuses[id]);
    if (statuses[id] === 'in_service') {
      sessionStorage.setItem('orbe-application-appointment', id);
      onNavigate('staff-application');
      return;
    }
    if (current < 3) {
      const next = order[current + 1];
      const apiStatuses: Partial<Record<QueueStatus, 'ESPERA'|'EM_ATENDIMENTO'|'CONCLUIDO'>> = {
        waiting:'ESPERA', in_service:'EM_ATENDIMENTO', completed:'CONCLUIDO',
      };
      const apiStatus = apiStatuses[next];
      if (!apiStatus) return;
      try {
        await staffApi.updateAppointmentStatus(Number(id), apiStatus);
        statuses = { ...statuses, [id]: next };
        toast = `Atendimento atualizado para “${labels[next]}”.`;
      } catch (exception) {
        toast = exception instanceof Error ? exception.message : 'Não foi possível atualizar o atendimento.';
      }
    }
  }
  onMount(async () => {
    try {
      const [agenda, patientList] = await Promise.all([staffApi.dailyAgenda(today), staffApi.patients()]);
      patients = patientList;
      const statusMap: Record<string, QueueStatus> = { PENDENTE:'confirmed', CONFIRMADO:'confirmed', ESPERA:'waiting', EM_ATENDIMENTO:'in_service', CONCLUIDO:'completed' };
      dailyAppointments = agenda.map((item) => ({
        id:String(item.id), time:new Date(item.dataAgendamento).toLocaleTimeString('pt-BR', {hour:'2-digit',minute:'2-digit'}),
        patient:item.paciente, cpf:item.cpf ? `***.***.***-${item.cpf.slice(-2)}` : 'Não informado',
        vaccine:item.vacina, vaccineId:item.vacinaId, usuarioId:item.usuarioId, dependenteId:item.dependenteId,
        dose:item.dose, room:item.sala || 'A confirmar', status:statusMap[item.status] ?? 'confirmed', tipoAtendimento:item.tipoAtendimento,
      }));
      statuses = Object.fromEntries(dailyAppointments.map((item) => [item.id, item.status]));
      const requested = sessionStorage.getItem('orbe-application-appointment');
      const candidate = dailyAppointments.find((item) => item.id === requested)
        ?? dailyAppointments.find((item) => item.status === 'in_service');
      if (candidate) await selectApplicationAppointment(candidate.id);
    } catch (exception) { agendaError = exception instanceof Error ? exception.message : 'Não foi possível carregar a agenda.'; }
    finally { agendaLoading = false; }
  });
  async function selectApplicationAppointment(id: string) {
    selectedPatient = id;
    const appointment = dailyAppointments.find((item) => item.id === id);
    selectedVaccine = appointment ? String(appointment.vaccineId) : '';
    dose = appointment?.dose ?? '';
    batch = '';
    applicationError = '';
    try {
      availableBatches = appointment ? await staffApi.batches(appointment.vaccineId) : [];
    } catch (exception) {
      availableBatches = [];
      applicationError = exception instanceof Error ? exception.message : 'Não foi possível carregar os lotes.';
    }
  }
  async function submit(e: SubmitEvent) {
    e.preventDefault();
    const appointment = dailyAppointments.find((item) => item.id === selectedPatient);
    if (!appointment || !batch || !dose || !staffUser) return;
    submittingApplication = true; applicationError = '';
    try {
      receipt = await staffApi.registerApplication({
        agendamentoId:Number(appointment.id), usuarioId:appointment.usuarioId,
        dependenteId:appointment.dependenteId, funcionarioId:staffUser.id,
        loteId:Number(batch), dose, dataAplicacao:`${applicationDate}T${applicationTime}:00`,
        tipoAtendimento:appointment.tipoAtendimento, viaAdministracao:administrationRoute,
        localAplicacao:applicationSite, valorPago:null, observacoes:null,
      });
      applied = true; sessionStorage.removeItem('orbe-application-appointment');
    } catch (exception) { applicationError = exception instanceof Error ? exception.message : 'Não foi possível registrar a aplicação.'; }
    finally { submittingApplication = false; }
  }
  async function savePatient(patient: StaffPatientInput, id?: string) {
    try {
      const saved = await staffApi.savePatient(patient, id);
      patients = id ? patients.map((item) => item.id === id ? saved : item) : [saved, ...patients];
      patientDialog = false; editingPatient = null;
      toast = id ? 'Paciente atualizado com sucesso.' : 'Paciente cadastrado com sucesso.';
    } catch (exception) {
      toast = exception instanceof Error ? exception.message : 'Não foi possível salvar o paciente.';
    }
  }
</script>

<div class="page">
  {#if mode === 'dashboard'}<PageHeader
      eyebrow="Operação da clínica"
      title={`Olá, ${staffUser?.nome.split(' ')[0] ?? 'profissional'}`}
      description="Acompanhe a operação e os atendimentos de hoje."
    />
    <div class="stats">
      {#each [{ n: String(dailyAppointments.length), l: 'Agendados hoje', c: '' }, { n: String(Object.values(statuses).filter(s=>s==='waiting').length), l: 'Na sala de espera', c: 'warning' }, { n: String(Object.values(statuses).filter(s=>s==='completed').length), l: 'Atendimentos concluídos', c: 'success' }] as item}<Card
          ><div class="stat {item.c}"><strong>{item.n}</strong><span>{item.l}</span></div></Card
        >{/each}
    </div>
    <div class="dashboard-grid">
      <section>
        <div class="heading">
          <div>
            <h2>Próximos atendimentos</h2>
            <p>Agenda da unidade Centro.</p>
          </div>
          <button onclick={() => onNavigate('staff-agenda')}>Ver agenda completa</button>
        </div>
        <div class="queue">
          {#if agendaError}<Alert tone="danger">{agendaError}</Alert>{/if}
          {#if agendaLoading}<p>Carregando agenda...</p>{/if}
          {#each dailyAppointments.slice(1, 5) as item}<article>
              <time>{item.time}</time>
              <div><strong>{item.patient}</strong><small>{item.vaccine} · {item.dose}</small></div>
              <span class={statuses[item.id]}>{labels[statuses[item.id]]}</span>
            </article>{/each}
        </div>
      </section>
      <aside>
        <h2>Ações rápidas</h2>
        <button onclick={() => onNavigate('staff-application')}
          ><span>✚</span><strong>Registrar aplicação</strong><small>Atestar vacina e consumir lote</small></button
        ><button
          onclick={() => {
            editingPatient = null;
            patientDialog = true;
          }}><span>♧</span><strong>Cadastrar paciente</strong><small>Novo titular ou dependente</small></button
        >
      </aside>
    </div>
  {:else if mode === 'agenda'}<PageHeader
      eyebrow="Atendimentos"
      title="Agenda do dia"
      description={`${new Date(`${today}T12:00:00`).toLocaleDateString('pt-BR', { dateStyle:'full' })} · Unidade Centro`}
      >{#snippet actions()}<Button
          variant="secondary"
          onclick={() => (toast = 'Exibindo a agenda de 22 de julho de 2026.')}>Calendário</Button
        ><Button onclick={() => onNavigate('staff-application')}>Novo encaixe</Button>{/snippet}</PageHeader
    >
    <div class="collection">
      <CollectionPanel title="Atendimentos do dia" description={`${dailyAppointments.length} horários na agenda`}>
        {#snippet actions()}<ViewModeToggle bind:value={agendaView} />{/snippet}
        <div class="toolbar">
          <input placeholder="Buscar paciente" /><select
            ><option>Todos os status</option><option>Confirmados</option><option>Na espera</option></select
          >
        </div>
        <div class="agenda {agendaView}">
          {#if agendaError}<Alert tone="danger">{agendaError}</Alert>{/if}
          {#if agendaLoading}<p>Carregando agenda...</p>{/if}
          {#each dailyAppointments as item}<article>
              <time>{item.time}</time>
              <div class="patient"><strong>{item.patient}</strong><small>{item.cpf}</small></div>
              <div><strong>{item.vaccine}</strong><small>{item.dose} · {item.room}</small></div>
              <span class={statuses[item.id]}>{labels[statuses[item.id]]}</span><Button
                size="sm"
                variant={statuses[item.id] === 'completed' ? 'secondary' : 'primary'}
                disabled={statuses[item.id] === 'completed'}
                onclick={() => advance(item.id)}
                >{statuses[item.id] === 'confirmed'
                  ? 'Fazer check-in'
                  : statuses[item.id] === 'waiting'
                    ? 'Iniciar'
                    : statuses[item.id] === 'in_service'
                      ? 'Registrar aplicação'
                      : 'Concluído'}</Button
              >
            </article>{/each}
        </div></CollectionPanel
      >
    </div>
  {:else if mode === 'patients'}<PageHeader
      eyebrow="Cadastros"
      title="Pacientes"
      description="Consulte históricos e mantenha os dados cadastrais atualizados."
      >{#snippet actions()}<Button
          onclick={() => {
            editingPatient = null;
            patientDialog = true;
          }}>Novo paciente</Button
        >{/snippet}</PageHeader
    >
    <div class="collection">
      <CollectionPanel title="Pacientes cadastrados" description={`${filteredPatients.length} registros encontrados`}>
        {#snippet actions()}<ViewModeToggle bind:value={patientView} />{/snippet}
        <div class="toolbar"><input placeholder="Buscar por nome ou CPF" bind:value={query} /></div>
        <div class="table {patientView}">
          <div class="tr head">
            <span>Paciente</span><span>CPF</span><span>Contato</span><span>Última vacina</span><span></span>
          </div>
          {#each filteredPatients as person}<div class="tr">
              <span><strong>{person.nome}</strong><small>{person.tipo === 'DEPENDENTE' ? 'Dependente' : 'Titular'} · Nascimento: {new Date(`${person.dataNascimento}T12:00:00`).toLocaleDateString('pt-BR')}</small></span><span
                >{person.cpf}</span
              ><span>{person.telefone ?? 'Vinculado ao responsável'}</span><span>{person.status === 'ATIVO' ? 'Ativo' : 'Inativo'}</span><button
                onclick={() => {
                  editingPatient = person;
                  patientDialog = true;
                }}>Editar →</button
              >
            </div>{/each}
        </div></CollectionPanel
      >
    </div>
  {:else}<PageHeader
      eyebrow="Registro clínico"
      title="Registrar aplicação"
      description="Confirme os dados da vacina aplicada e o lote utilizado."
    />{#if applied && receipt}<div class="success">
        <Alert tone="success"
          ><strong>Aplicação registrada.</strong> O estoque do lote foi atualizado e o evento entrou na auditoria.</Alert
        ><Card
          ><div class="receipt">
            <p>Comprovante de aplicação</p>
            <h2>{selectedClinicalAppointment?.vaccine}</h2>
            <dl>
              <div>
                <dt>Protocolo</dt>
                <dd>{receipt.protocolo}</dd>
              </div>
              <div>
                <dt>Paciente</dt>
                <dd>{selectedClinicalAppointment?.patient}</dd>
              </div>
              <div>
                <dt>Dose e lote</dt>
                <dd>{receipt.dose} · {selectedBatch?.numeroLote}</dd>
              </div>
              <div>
                <dt>Data e horário</dt>
                <dd>{new Date(receipt.dataAplicacao).toLocaleString('pt-BR')}</dd>
              </div>
              <div>
                <dt>Profissional</dt>
                <dd>{staffUser?.nome}</dd>
              </div>
              <div>
                <dt>Local</dt>
                <dd>{receipt.localAplicacao}</dd>
              </div>
            </dl>
          </div></Card
        >
        <div class="receipt-actions">
          <Button variant="secondary" onclick={() => window.print()}>Imprimir comprovante</Button><Button
            onclick={() => {
              applied = false;
              receipt = null;
              selectedPatient = '';
              selectedVaccine = '';
              batch = '';
              dose = '';
            }}>Registrar outra aplicação</Button
          >
        </div>
      </div>{:else}<form onsubmit={submit}>
        <Card padding="lg"
          ><div class="form-title">
            <h2>Paciente e vacina</h2>
            <p>Todos os campos marcados são obrigatórios.</p>
          </div>
          <div class="form-grid">
            <label
              >Atendimento<select bind:value={selectedPatient} onchange={() => selectApplicationAppointment(selectedPatient)} required
                ><option value="">Selecione</option>{#each dailyAppointments.filter((p) => p.status === 'in_service') as p}<option
                    value={p.id}>{p.time} · {p.patient} · {p.vaccine}</option
                  >{/each}</select
              ></label
            ><label
              >Vacina<select bind:value={selectedVaccine} disabled required
                ><option value="">Selecione o atendimento</option>{#if selectedClinicalAppointment}<option value={String(selectedClinicalAppointment.vaccineId)}>{selectedClinicalAppointment.vaccine}</option>{/if}</select
              ></label
            ><label
              >Lote<select bind:value={batch} required
                ><option value="">Selecione</option>{#each availableBatches as item}<option value={String(item.id)}>{item.numeroLote} · Val. {new Date(`${item.dataValidade}T12:00:00`).toLocaleDateString('pt-BR')} · {item.quantidadeAtual} doses</option>{/each}</select
              ></label
            ><FormField
              id="dose"
              label="Dose"
              placeholder="Ex.: 2ª dose"
              value={dose}
              oninput={(v) => (dose = v)}
              required
            />
          </div></Card
        ><Card padding="lg"
          ><div class="form-title">
            <h2>Dados da aplicação</h2>
            <p>Informações que constarão na carteira vacinal.</p>
          </div>
          <div class="form-grid">
            <FormField id="application-date" label="Data" type="date" value={applicationDate} oninput={(v) => (applicationDate = v)} /><FormField
              id="application-time"
              label="Horário"
              value={applicationTime}
              oninput={(v) => (applicationTime = v)}
            /><label
              >Tipo de atendimento<select
                ><option>Agendado</option><option>Encaixe</option><option>Domiciliar</option></select
              ></label
            ><label
              >Via de administração<select bind:value={administrationRoute}
                ><option>Intramuscular</option><option>Subcutânea</option><option>Oral</option></select
              ></label
            ><FormField id="site" label="Local de aplicação" value={applicationSite} oninput={(v) => (applicationSite = v)} /><FormField
              id="professional"
              label="Profissional"
              value={staffUser?.nome ?? ''}
              disabled
            />
          </div></Card
        ><Alert tone="danger"
          >Confira vacina, dose e lote antes de concluir. O registro clínico exigirá correção auditada após a
          confirmação.</Alert
        >
        {#if applicationError}<Alert tone="danger">{applicationError}</Alert>{/if}
        <div class="submit">
          <Button variant="secondary" onclick={() => onNavigate('staff-agenda')}>Cancelar</Button><Button type="submit" disabled={submittingApplication}
            >{submittingApplication ? 'Registrando...' : 'Confirmar aplicação'}</Button
          >
        </div>
      </form>{/if}{/if}
</div>
{#if patientDialog}<PatientDialog
    initial={editingPatient ?? undefined}
    holders={patients.filter((person) => person.tipo === 'TITULAR')}
    onSave={savePatient}
    onCancel={() => {
      patientDialog = false;
      editingPatient = null;
    }}
  />{/if}
{#if toast}<Toast message={toast} onClose={() => (toast = '')} />{/if}

<style>
  .page {
    width: min(100%, var(--content-max));
    margin: 0 auto;
    padding: var(--space-8);
  }
  .stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-4);
    margin-top: var(--space-6);
  }
  .stat {
    display: grid;
    gap: var(--space-2);
  }
  .stat strong {
    font-size: var(--text-3xl);
  }
  .stat span {
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
  .stat.warning strong {
    color: var(--status-warning);
  }
  .stat.success strong {
    color: var(--status-success);
  }
  .stat.danger strong {
    color: var(--status-danger);
  }
  .dashboard-grid {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: var(--space-5);
    margin-top: var(--space-6);
  }
  .dashboard-grid > section,
  .dashboard-grid > aside {
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-lg);
    background: var(--surface-card);
    padding: var(--space-5);
  }
  .heading {
    display: flex;
    justify-content: space-between;
  }
  .heading h2,
  .dashboard-grid aside h2 {
    font-size: var(--text-lg);
  }
  .heading p {
    margin-top: 0.25rem;
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
  .heading button {
    border: 0;
    background: transparent;
    color: var(--color-brand-500);
    font-weight: 700;
    cursor: pointer;
  }
  .queue {
    margin-top: var(--space-4);
  }
  .queue article {
    display: grid;
    grid-template-columns: auto 1fr auto;
    align-items: center;
    gap: var(--space-4);
    border-top: 1px solid var(--border-subtle);
    padding: var(--space-4) 0;
  }
  .queue time {
    font-weight: 800;
  }
  .queue article > div,
  .dashboard-grid aside button {
    display: grid;
    gap: 0.2rem;
  }
  small {
    color: var(--text-secondary);
  }
  .queue article > span,
  .agenda article > span {
    border-radius: var(--radius-pill);
    padding: 0.3rem 0.6rem;
    font-size: var(--text-xs);
    font-weight: 750;
  }
  .confirmed {
    background: var(--color-brand-50);
    color: var(--color-brand-700);
  }
  .waiting {
    background: var(--status-warning-bg);
    color: var(--status-warning);
  }
  .in_service {
    background: var(--surface-subtle);
    color: var(--text-primary);
  }
  .completed {
    background: var(--status-success-bg);
    color: var(--status-success);
  }
  .dashboard-grid aside {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }
  .dashboard-grid aside h2 {
    margin-bottom: var(--space-2);
  }
  .dashboard-grid aside button {
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: var(--space-4);
    text-align: left;
    cursor: pointer;
  }
  .dashboard-grid aside button > span {
    color: var(--color-brand-500);
    font-size: 1.3rem;
  }
  .toolbar {
    display: flex;
    gap: var(--space-3);
    margin: 0 0 var(--space-4);
  }
  .collection {
    margin-top: var(--space-6);
  }
  input,
  select {
    min-height: 2.75rem;
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: 0 var(--space-4);
    color: var(--text-primary);
  }
  .toolbar input {
    width: min(100%, 25rem);
  }
  .agenda,
  .table {
    overflow: hidden;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-lg);
    background: var(--surface-card);
  }
  .agenda article {
    display: grid;
    grid-template-columns: 4rem 1fr 1.5fr auto auto;
    align-items: center;
    gap: var(--space-5);
    border-bottom: 1px solid var(--border-subtle);
    padding: var(--space-4) var(--space-5);
  }
  .agenda article:last-child {
    border: 0;
  }
  .agenda article > div {
    display: grid;
    gap: 0.25rem;
  }
  .agenda.grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-4);
    overflow: visible;
    border: 0;
  }
  .agenda.grid article {
    grid-template-columns: 3rem 1fr auto;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-4);
  }
  .agenda.grid article > div:nth-of-type(2) {
    grid-column: 2 / -1;
  }
  .agenda.grid article > :global(button) {
    grid-column: 2 / -1;
    justify-self: start;
  }
  .tr {
    display: grid;
    grid-template-columns: 1.3fr 1fr 1fr 1.4fr auto;
    align-items: center;
    gap: var(--space-4);
    border-bottom: 1px solid var(--border-subtle);
    padding: var(--space-4) var(--space-5);
    font-size: var(--text-sm);
  }
  .tr:last-child {
    border: 0;
  }
  .tr.head {
    background: var(--surface-subtle);
    color: var(--text-secondary);
    font-size: var(--text-xs);
    font-weight: 750;
  }
  .tr > span {
    display: grid;
    gap: 0.25rem;
  }
  .tr button {
    border: 0;
    background: transparent;
    color: var(--color-brand-500);
    font-weight: 700;
    cursor: pointer;
  }
  .table.grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-4);
    overflow: visible;
    border: 0;
  }
  .table.grid .tr.head {
    display: none;
  }
  .table.grid .tr {
    grid-template-columns: 1fr auto;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-4);
  }
  .table.grid .tr > span:nth-child(n + 2) {
    grid-column: 1;
  }
  .table.grid .tr button {
    grid-column: 2;
    grid-row: 1;
  }
  form {
    display: grid;
    gap: var(--space-4);
    margin-top: var(--space-6);
  }
  .form-title {
    margin-bottom: var(--space-6);
  }
  .form-title h2 {
    font-size: var(--text-lg);
  }
  .form-title p {
    margin-top: 0.3rem;
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
  .form-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-5);
  }
  .form-grid > label {
    display: grid;
    gap: var(--space-2);
    font-size: var(--text-sm);
    font-weight: 650;
  }
  .submit {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);
  }
  .success {
    display: grid;
    max-width: 40rem;
    gap: var(--space-5);
    margin-top: var(--space-6);
  }
  @media (max-width: 1000px) {
    .agenda.grid,
    .table.grid {
      grid-template-columns: 1fr;
    }
    .stats {
      grid-template-columns: repeat(2, 1fr);
    }
    .dashboard-grid {
      grid-template-columns: 1fr;
    }
    .agenda article {
      grid-template-columns: 4rem 1fr auto;
    }
    .agenda article > div:nth-of-type(2) {
      display: none;
    }
    .tr {
      grid-template-columns: 1fr 1fr auto;
    }
    .tr span:nth-child(3),
    .tr span:nth-child(4) {
      display: none;
    }
  }
  @media (max-width: 680px) {
    .page {
      padding: var(--space-5);
    }
    .stats,
    .form-grid {
      grid-template-columns: 1fr;
    }
    .agenda article {
      grid-template-columns: 3rem 1fr;
    }
    .agenda article > span,
    .agenda article > :global(button) {
      grid-column: 2;
    }
    .tr {
      grid-template-columns: 1fr auto;
    }
    .tr span:nth-child(2) {
      display: none;
    }
    .toolbar {
      align-items: stretch;
      flex-direction: column;
    }
    .toolbar input {
      width: 100%;
    }
  }
</style>
