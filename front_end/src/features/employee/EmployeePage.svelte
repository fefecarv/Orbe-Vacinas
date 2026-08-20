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
  import StatusBadge from '../../design-system/components/StatusBadge.svelte';
  import { canAdvanceStatus, localDateValue, mapEmployeeStatus, type QueueStatus } from './employeeLogic';
  import {
    currentUser,
    staffApi,
    type ApiBatch,
    type ApiApplication,
    type ApiVaccine,
    type RegisteredApplication,
    type StaffPatient,
    type StaffPatientInput,
  } from '../../lib/api';
  let {
    mode,
    onNavigate,
  }: { mode: 'dashboard' | 'agenda' | 'patients' | 'application'; onNavigate: (page: string) => void } = $props();
  type DailyAppointment = {
    id: string;
    time: string;
    patient: string;
    cpf: string;
    vaccine: string;
    vaccineId: number;
    usuarioId: number | null;
    dependenteId: number | null;
    dose: string;
    room: string;
    status: QueueStatus;
    tipoAtendimento: 'PARTICULAR' | 'CONVENIO' | 'CAMPANHA';
  };
  let dailyAppointments = $state<DailyAppointment[]>([]);
  let statuses = $state<Record<string, QueueStatus>>({});
  let patients = $state<StaffPatient[]>([]);
  let query = $state('');
  let agendaQuery = $state('');
  let agendaStatus = $state('all');
  let patientStatus = $state('all');
  let applied = $state(false);
  let selectedPatient = $state('');
  let selectedVaccine = $state('');
  let batch = $state('');
  let dose = $state('');
  let receipt = $state<RegisteredApplication | null>(null);
  let availableBatches = $state<ApiBatch[]>([]);
  let availableVaccines = $state<ApiVaccine[]>([]);
  let applicationMode = $state<'scheduled' | 'walkin'>(
    sessionStorage.getItem('orbe-application-mode') === 'walkin' ? 'walkin' : 'scheduled',
  );
  let walkinPatient = $state('');
  let walkinVaccine = $state('');
  let walkinAttendance = $state<'PARTICULAR' | 'CAMPANHA'>('PARTICULAR');
  let applicationDate = $state(localDateValue());
  let applicationTime = $state(new Date().toTimeString().slice(0, 5));
  let administrationRoute = $state('Intramuscular');
  let applicationSite = $state('Deltoide direito');
  let observations = $state('');
  let identityConfirmed = $state(false);
  let screeningConfirmed = $state(false);
  let applicationError = $state('');
  let submittingApplication = $state(false);
  let patientDialog = $state(false);
  let editingPatient = $state<StaffPatient | null>(null);
  let historyPatient = $state<StaffPatient | null>(null);
  let patientHistory = $state<ApiApplication[]>([]);
  let historyLoading = $state(false);
  let toast = $state('');
  let agendaLoading = $state(true);
  let agendaError = $state('');
  const staffUser = currentUser();
  const today = localDateValue();
  let agendaDate = $state(today);
  const staffUnit = staffUser?.unidade || 'Orbe Centro';
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
    canceled: 'Cancelado',
    missed: 'Faltou',
  };
  let filteredPatients = $derived(
    patients.filter(
      (p) =>
        (p.nome.toLowerCase().includes(query.toLowerCase()) || (p.cpf ?? '').includes(query.replace(/\D/g, ''))) &&
        (patientStatus === 'all' || p.status === patientStatus),
    ),
  );
  let filteredAgenda = $derived(
    dailyAppointments.filter((item) => {
      const matchesQuery =
        item.patient.toLowerCase().includes(agendaQuery.toLowerCase()) ||
        item.cpf.includes(agendaQuery.replace(/\D/g, ''));
      return matchesQuery && (agendaStatus === 'all' || item.status === agendaStatus);
    }),
  );
  let selectedClinicalAppointment = $derived(dailyAppointments.find((item) => item.id === selectedPatient));
  let selectedWalkinPatient = $derived(patients.find((item) => item.id === walkinPatient));
  let selectedWalkinVaccine = $derived(availableVaccines.find((item) => String(item.id) === walkinVaccine));
  let selectedBatch = $derived(availableBatches.find((item) => String(item.id) === batch));
  async function advance(id: string) {
    const order: QueueStatus[] = ['confirmed', 'waiting', 'in_service', 'completed'];
    const current = order.indexOf(statuses[id]);
    if (statuses[id] === 'in_service') {
      sessionStorage.setItem('orbe-application-appointment', id);
      onNavigate('staff-application');
      return;
    }
    if (current < 3 && canAdvanceStatus(statuses[id])) {
      const next = order[current + 1];
      const apiStatuses: Partial<Record<QueueStatus, 'ESPERA' | 'EM_ATENDIMENTO' | 'CONCLUIDO'>> = {
        waiting: 'ESPERA',
        in_service: 'EM_ATENDIMENTO',
        completed: 'CONCLUIDO',
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
  async function loadAgenda() {
    agendaLoading = true;
    agendaError = '';
    try {
      const agenda = await staffApi.dailyAgenda(agendaDate, staffUnit);
      dailyAppointments = agenda.map((item) => ({
        id: String(item.id),
        time: new Date(item.dataAgendamento).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
        patient: item.paciente,
        cpf: item.cpf ? `***.***.***-${item.cpf.slice(-2)}` : 'Não informado',
        vaccine: item.vacina,
        vaccineId: item.vacinaId,
        usuarioId: item.usuarioId,
        dependenteId: item.dependenteId,
        dose: item.dose,
        room: item.sala || 'A confirmar',
        status: mapEmployeeStatus(item.status),
        tipoAtendimento: item.tipoAtendimento,
      }));
      statuses = Object.fromEntries(dailyAppointments.map((item) => [item.id, item.status]));
      const requested = sessionStorage.getItem('orbe-application-appointment');
      const candidate =
        dailyAppointments.find((item) => item.id === requested) ??
        dailyAppointments.find((item) => item.status === 'in_service');
      if (candidate) await selectApplicationAppointment(candidate.id);
    } catch (exception) {
      agendaError = exception instanceof Error ? exception.message : 'Não foi possível carregar a agenda.';
    } finally {
      agendaLoading = false;
    }
  }
  async function changeAgendaDay(days: number) {
    const date = new Date(`${agendaDate}T12:00:00`);
    date.setDate(date.getDate() + days);
    agendaDate = localDateValue(date);
    await loadAgenda();
  }
  onMount(async () => {
    await Promise.all([
      loadAgenda(),
      staffApi
        .patients()
        .then((items) => (patients = items))
        .catch((exception) => {
          toast = exception instanceof Error ? exception.message : 'Não foi possível carregar os pacientes.';
        }),
      staffApi
        .vaccines()
        .then((items) => (availableVaccines = items))
        .catch(() => undefined),
    ]);
  });
  async function selectApplicationAppointment(id: string) {
    selectedPatient = id;
    let appointment = dailyAppointments.find((item) => item.id === id);
    if (appointment?.status === 'waiting') {
      try {
        await staffApi.updateAppointmentStatus(Number(id), 'EM_ATENDIMENTO');
        statuses = { ...statuses, [id]: 'in_service' };
        dailyAppointments = dailyAppointments.map((item) =>
          item.id === id ? { ...item, status: 'in_service' } : item,
        );
        appointment = dailyAppointments.find((item) => item.id === id);
        toast = 'Atendimento iniciado. Confira os dados antes de registrar a aplicação.';
      } catch (exception) {
        applicationError = exception instanceof Error ? exception.message : 'Não foi possível iniciar o atendimento.';
        return;
      }
    }
    selectedVaccine = appointment ? String(appointment.vaccineId) : '';
    dose = appointment?.dose ?? '';
    batch = '';
    applicationError = '';
    try {
      availableBatches = appointment ? await staffApi.batches(appointment.vaccineId) : [];
      batch = availableBatches[0] ? String(availableBatches[0].id) : '';
    } catch (exception) {
      availableBatches = [];
      applicationError = exception instanceof Error ? exception.message : 'Não foi possível carregar os lotes.';
    }
  }
  async function selectWalkinVaccine(id: string) {
    walkinVaccine = id;
    batch = '';
    applicationError = '';
    const vaccine = availableVaccines.find((item) => String(item.id) === id);
    dose = vaccine?.numeroDoses === 1 ? 'Dose única' : '1ª dose';
    try {
      availableBatches = id ? await staffApi.batches(Number(id)) : [];
      batch = availableBatches[0] ? String(availableBatches[0].id) : '';
    } catch (exception) {
      availableBatches = [];
      applicationError = exception instanceof Error ? exception.message : 'Não foi possível carregar os lotes.';
    }
  }
  async function submit(e: SubmitEvent) {
    e.preventDefault();
    const appointment =
      applicationMode === 'scheduled' ? dailyAppointments.find((item) => item.id === selectedPatient) : undefined;
    const patient = applicationMode === 'walkin' ? patients.find((item) => item.id === walkinPatient) : undefined;
    if ((!appointment && !patient) || !batch || !dose || !staffUser || !identityConfirmed || !screeningConfirmed) {
      applicationError = 'Confirme a identidade do paciente e a triagem pré-vacinal.';
      return;
    }
    submittingApplication = true;
    applicationError = '';
    try {
      receipt = await staffApi.registerApplication({
        agendamentoId: appointment ? Number(appointment.id) : null,
        usuarioId: appointment?.usuarioId ?? (patient?.tipo === 'TITULAR' ? Number(patient.id.split(':')[1]) : null),
        dependenteId:
          appointment?.dependenteId ?? (patient?.tipo === 'DEPENDENTE' ? Number(patient.id.split(':')[1]) : null),
        funcionarioId: staffUser.id,
        loteId: Number(batch),
        dose,
        dataAplicacao: `${applicationDate}T${applicationTime}:00`,
        tipoAtendimento: appointment?.tipoAtendimento ?? walkinAttendance,
        viaAdministracao: administrationRoute,
        localAplicacao: applicationSite,
        valorPago: null,
        observacoes: observations || null,
      });
      applied = true;
      sessionStorage.removeItem('orbe-application-appointment');
      sessionStorage.removeItem('orbe-application-mode');
    } catch (exception) {
      applicationError = exception instanceof Error ? exception.message : 'Não foi possível registrar a aplicação.';
    } finally {
      submittingApplication = false;
    }
  }
  async function savePatient(patient: StaffPatientInput, id?: string) {
    try {
      const saved = await staffApi.savePatient(patient, id);
      patients = id ? patients.map((item) => (item.id === id ? saved : item)) : [saved, ...patients];
      patientDialog = false;
      editingPatient = null;
      toast = id ? 'Paciente atualizado com sucesso.' : 'Paciente cadastrado com sucesso.';
    } catch (exception) {
      toast = exception instanceof Error ? exception.message : 'Não foi possível salvar o paciente.';
    }
  }
  async function openPatientHistory(patient: StaffPatient) {
    historyPatient = patient;
    patientHistory = [];
    historyLoading = true;
    try {
      patientHistory = await staffApi.patientHistory(patient.id);
    } catch (exception) {
      toast = exception instanceof Error ? exception.message : 'Não foi possível carregar o histórico.';
      historyPatient = null;
    } finally {
      historyLoading = false;
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
      {#each [{ n: String(dailyAppointments.filter((item) => !['canceled', 'missed'].includes(statuses[item.id])).length), l: 'Atendimentos hoje', c: '' }, { n: String(Object.values(statuses).filter((s) => s === 'waiting').length), l: 'Na sala de espera', c: 'warning' }, { n: String(Object.values(statuses).filter((s) => s === 'in_service').length), l: 'Em atendimento', c: '' }, { n: String(Object.values(statuses).filter((s) => s === 'completed').length), l: 'Concluídos', c: 'success' }] as item}<Card
          ><div class="stat {item.c}"><strong>{item.n}</strong><span>{item.l}</span></div></Card
        >{/each}
    </div>
    <div class="dashboard-grid">
      <section>
        <div class="heading">
          <div>
            <h2>Próximos atendimentos</h2>
            <p>Agenda da unidade {staffUnit}.</p>
          </div>
          <button onclick={() => onNavigate('staff-agenda')}>Ver agenda completa</button>
        </div>
        <div class="queue">
          {#if agendaError}<Alert tone="danger">{agendaError}</Alert>{/if}
          {#if agendaLoading}<p>Carregando agenda...</p>{/if}
          {#each dailyAppointments
            .filter((item) => ['confirmed', 'waiting', 'in_service'].includes(statuses[item.id]))
            .slice(0, 4) as item}<article>
              <time>{item.time}</time>
              <div><strong>{item.patient}</strong><small>{item.vaccine} · {item.dose}</small></div>
              <StatusBadge status={statuses[item.id]} />
            </article>{/each}
          {#if !agendaLoading && dailyAppointments.filter( (item) => ['confirmed', 'waiting', 'in_service'].includes(statuses[item.id]) ).length === 0}<p
              class="empty"
            >
              Nenhum atendimento aguardando.
            </p>{/if}
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
      description={`${new Date(`${agendaDate}T12:00:00`).toLocaleDateString('pt-BR', { dateStyle: 'full' })} · ${staffUnit}`}
      >{#snippet actions()}<Button variant="secondary" onclick={() => changeAgendaDay(-1)}>← Dia anterior</Button
        ><Button variant="secondary" onclick={() => changeAgendaDay(1)}>Próximo dia →</Button><Button
          onclick={() => {
            sessionStorage.setItem('orbe-application-mode', 'walkin');
            onNavigate('staff-application');
          }}>Novo encaixe</Button
        >{/snippet}</PageHeader
    >
    <div class="collection">
      <CollectionPanel title="Atendimentos do dia" description={`${dailyAppointments.length} horários na agenda`}>
        {#snippet actions()}<ViewModeToggle bind:value={agendaView} />{/snippet}
        <div class="toolbar">
          <input type="date" bind:value={agendaDate} onchange={loadAgenda} aria-label="Data da agenda" />
          <input placeholder="Buscar paciente ou CPF" bind:value={agendaQuery} />
          <select bind:value={agendaStatus} aria-label="Filtrar por status"
            ><option value="all">Todos os status</option><option value="confirmed">Confirmados</option><option
              value="waiting">Na espera</option
            ><option value="in_service">Em atendimento</option><option value="completed">Concluídos</option><option
              value="canceled">Cancelados</option
            ><option value="missed">Faltas</option></select
          >
        </div>
        <div class="agenda {agendaView}">
          {#if agendaError}<Alert tone="danger">{agendaError}</Alert>{/if}
          {#if agendaLoading}<p>Carregando agenda...</p>{/if}
          {#each filteredAgenda as item}<article>
              <time>{item.time}</time>
              <div class="patient"><strong>{item.patient}</strong><small>{item.cpf}</small></div>
              <div><strong>{item.vaccine}</strong><small>{item.dose} · {item.room}</small></div>
              <StatusBadge status={statuses[item.id]} /><Button
                size="sm"
                variant={statuses[item.id] === 'completed' ? 'secondary' : 'primary'}
                disabled={['completed', 'canceled', 'missed'].includes(statuses[item.id])}
                onclick={() => advance(item.id)}
                >{statuses[item.id] === 'confirmed'
                  ? 'Fazer check-in'
                  : statuses[item.id] === 'waiting'
                    ? 'Iniciar'
                    : statuses[item.id] === 'in_service'
                      ? 'Registrar aplicação'
                      : statuses[item.id] === 'canceled'
                        ? 'Cancelado'
                        : statuses[item.id] === 'missed'
                          ? 'Não compareceu'
                          : 'Concluído'}</Button
              >
            </article>{/each}
          {#if !agendaLoading && filteredAgenda.length === 0}<p class="empty">
              Nenhum atendimento encontrado para os filtros selecionados.
            </p>{/if}
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
        <div class="toolbar">
          <input placeholder="Buscar por nome ou CPF" bind:value={query} /><select bind:value={patientStatus}
            ><option value="all">Todas as situações</option><option value="ATIVO">Ativos</option><option value="INATIVO"
              >Inativos</option
            ></select
          >
        </div>
        <div class="table {patientView}">
          <div class="tr head">
            <span>Paciente</span><span>CPF</span><span>Contato</span><span>Última vacina</span><span></span>
          </div>
          {#each filteredPatients as person}<div class="tr">
              <span
                ><strong>{person.nome}</strong><small
                  >{person.tipo === 'DEPENDENTE' ? 'Dependente' : 'Titular'} · Nascimento: {new Date(
                    `${person.dataNascimento}T12:00:00`,
                  ).toLocaleDateString('pt-BR')}</small
                ></span
              ><span>{person.cpf ?? 'Não informado'}</span><span>{person.telefone ?? 'Vinculado ao responsável'}</span
              ><span
                ><strong>{person.ultimaVacina ?? 'Nenhuma aplicação'}</strong><small
                  >{person.ultimaAplicacao
                    ? new Date(`${person.ultimaAplicacao}T12:00:00`).toLocaleDateString('pt-BR')
                    : person.status === 'ATIVO'
                      ? 'Cadastro ativo'
                      : 'Cadastro inativo'}</small
                ></span
              >
              <div class="row-actions">
                <button onclick={() => openPatientHistory(person)}>Histórico</button><button
                  onclick={() => {
                    editingPatient = person;
                    patientDialog = true;
                  }}>Editar →</button
                >
              </div>
            </div>{/each}
          {#if filteredPatients.length === 0}<p class="empty">Nenhum paciente encontrado.</p>{/if}
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
            <h2>{selectedClinicalAppointment?.vaccine ?? selectedWalkinVaccine?.nome}</h2>
            <dl>
              <div>
                <dt>Protocolo</dt>
                <dd>{receipt.protocolo}</dd>
              </div>
              <div>
                <dt>Paciente</dt>
                <dd>{selectedClinicalAppointment?.patient ?? selectedWalkinPatient?.nome}</dd>
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
              walkinPatient = '';
              walkinVaccine = '';
              batch = '';
              dose = '';
              identityConfirmed = false;
              screeningConfirmed = false;
              observations = '';
            }}>Registrar outra aplicação</Button
          >
        </div>
      </div>{:else}<form onsubmit={submit}>
        <div class="mode-switch" role="group" aria-label="Origem da aplicação">
          <button
            type="button"
            class:active={applicationMode === 'scheduled'}
            onclick={() => (applicationMode = 'scheduled')}>Atendimento agendado</button
          ><button
            type="button"
            class:active={applicationMode === 'walkin'}
            onclick={() => (applicationMode = 'walkin')}>Encaixe</button
          >
        </div>
        <Card padding="md"
          ><div class="form-title">
            <h2>Paciente e vacina</h2>
            <p>Todos os campos marcados são obrigatórios.</p>
          </div>
          <div class="form-grid">
            {#if applicationMode === 'scheduled'}<label
                >Atendimento<select
                  bind:value={selectedPatient}
                  onchange={() => selectApplicationAppointment(selectedPatient)}
                  required
                  ><option value="">Selecione</option
                  >{#each dailyAppointments.filter((p) => ['waiting', 'in_service'].includes(p.status)) as p}<option
                      value={p.id}>{p.time} · {p.patient} · {p.vaccine}</option
                    >{/each}</select
                ></label
              ><label
                >Vacina<select bind:value={selectedVaccine} disabled
                  ><option value="">Selecione o atendimento</option>{#if selectedClinicalAppointment}<option
                      value={String(selectedClinicalAppointment.vaccineId)}
                      >{selectedClinicalAppointment.vaccine}</option
                    >{/if}</select
                ></label
              >{:else}<label
                >Paciente<select bind:value={walkinPatient} required
                  ><option value="">Selecione</option
                  >{#each patients.filter((person) => person.status === 'ATIVO') as person}<option value={person.id}
                      >{person.nome} · {person.tipo === 'TITULAR' ? 'Titular' : 'Dependente'}</option
                    >{/each}</select
                ></label
              ><label
                >Vacina<select bind:value={walkinVaccine} onchange={() => selectWalkinVaccine(walkinVaccine)} required
                  ><option value="">Selecione</option>{#each availableVaccines as vaccine}<option
                      value={String(vaccine.id)}>{vaccine.nome} · {vaccine.fabricante}</option
                    >{/each}</select
                ></label
              >{/if}
            <label
              >Lote<select bind:value={batch} required
                ><option value="">Selecione</option>{#each availableBatches as item}<option value={String(item.id)}
                    >{item.numeroLote} · Val. {new Date(`${item.dataValidade}T12:00:00`).toLocaleDateString('pt-BR')} · {item.quantidadeAtual}
                    doses</option
                  >{/each}</select
              ></label
            >
            <label
              >Dose<select bind:value={dose} disabled={applicationMode === 'scheduled'} required
                ><option value="">Selecione</option>{#if applicationMode === 'scheduled' && dose}<option value={dose}
                    >{dose}</option
                  >{:else if selectedWalkinVaccine}{#if selectedWalkinVaccine.numeroDoses === 1}<option
                      >Dose única</option
                    >{:else}{#each Array.from({ length: selectedWalkinVaccine.numeroDoses }, (_, index) => `${index + 1}ª dose`) as option}<option
                        >{option}</option
                      >{/each}{/if}{/if}</select
              ></label
            >
          </div></Card
        >
        <Card padding="md"
          ><div class="form-title">
            <h2>Dados da aplicação</h2>
            <p>Informações que constarão na carteira vacinal.</p>
          </div>
          <div class="form-grid">
            <FormField
              id="application-date"
              label="Data"
              type="date"
              value={applicationDate}
              oninput={(v) => (applicationDate = v)}
            /><FormField
              id="application-time"
              label="Horário"
              type="time"
              value={applicationTime}
              oninput={(v) => (applicationTime = v)}
            />{#if applicationMode === 'scheduled'}<label
                >Cobertura<select disabled
                  ><option
                    >{selectedClinicalAppointment?.tipoAtendimento === 'CONVENIO'
                      ? 'Convênio'
                      : selectedClinicalAppointment?.tipoAtendimento === 'CAMPANHA'
                        ? 'Campanha'
                        : 'Particular'}</option
                  ></select
                ></label
              >{:else}<label
                >Cobertura<select bind:value={walkinAttendance}
                  ><option value="PARTICULAR">Particular</option><option value="CAMPANHA">Campanha</option></select
                ></label
              >{/if}
            ><label
              >Via de administração<select bind:value={administrationRoute}
                ><option>Intramuscular</option><option>Subcutânea</option><option>Oral</option></select
              ></label
            ><FormField
              id="site"
              label="Local de aplicação"
              value={applicationSite}
              oninput={(v) => (applicationSite = v)}
            /><FormField id="professional" label="Profissional" value={staffUser?.nome ?? ''} disabled />
          </div>
          <label class="observations"
            >Observações clínicas<textarea
              bind:value={observations}
              maxlength="500"
              placeholder="Registre orientações, intercorrências ou informações relevantes."></textarea></label
          ></Card
        >
        <Card padding="md"
          ><div class="checks">
            <label
              ><input type="checkbox" bind:checked={identityConfirmed} /> Identidade do paciente e responsável confirmadas</label
            ><label
              ><input type="checkbox" bind:checked={screeningConfirmed} /> Triagem pré-vacinal realizada, sem impedimentos
              informados</label
            >
          </div></Card
        >
        <Alert tone="info"
          >Confira paciente, vacina, dose, lote e validade antes de concluir. O registro será permanente e auditado.</Alert
        >
        {#if applicationError}<Alert tone="danger">{applicationError}</Alert>{/if}
        <div class="submit">
          <Button variant="secondary" onclick={() => onNavigate('staff-agenda')}>Cancelar</Button><Button
            type="submit"
            disabled={submittingApplication}>{submittingApplication ? 'Registrando...' : 'Confirmar aplicação'}</Button
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
{#if historyPatient}<div
    class="history-backdrop"
    role="presentation"
    onclick={(event) => event.target === event.currentTarget && (historyPatient = null)}
  >
    <div class="history-dialog" role="dialog" aria-modal="true" aria-labelledby="history-title">
      <header>
        <div>
          <p>Prontuário vacinal</p>
          <h2 id="history-title">{historyPatient.nome}</h2>
        </div>
        <button aria-label="Fechar" onclick={() => (historyPatient = null)}>×</button>
      </header>
      {#if historyLoading}<p>Carregando histórico...</p>{:else if patientHistory.length === 0}<p class="empty">
          Nenhuma aplicação registrada.
        </p>{:else}<div class="history-list">
          {#each patientHistory as item}<article>
              <div><strong>{item.vacina}</strong><small>{item.fabricante} · {item.dose}</small></div>
              <div>
                <strong>{new Date(item.dataAplicacao).toLocaleDateString('pt-BR')}</strong><small
                  >Lote {item.numeroLote}</small
                >
              </div>
              <div><strong>{item.profissional}</strong><small>{item.localAplicacao}</small></div>
            </article>{/each}
        </div>{/if}
      <footer>
        <Button variant="secondary" onclick={() => window.print()}>Imprimir</Button><Button
          onclick={() => (historyPatient = null)}>Fechar</Button
        >
      </footer>
    </div>
  </div>{/if}
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
  .row-actions {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }
  .history-backdrop {
    position: fixed;
    z-index: 90;
    inset: 0;
    display: grid;
    place-items: center;
    background: var(--surface-overlay);
    padding: var(--space-5);
  }
  .history-dialog {
    width: min(100%, 52rem);
    max-height: calc(100dvh - 2rem);
    overflow: auto;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-lg);
    background: var(--surface-card);
    padding: var(--space-5);
    box-shadow: var(--shadow-md);
  }
  .history-dialog header {
    display: flex;
    justify-content: space-between;
    margin-bottom: var(--space-4);
  }
  .history-dialog header p {
    color: var(--color-brand-500);
    font-size: var(--text-xs);
    font-weight: 800;
    text-transform: uppercase;
  }
  .history-dialog header button {
    border: 0;
    background: transparent;
    color: var(--text-secondary);
    font-size: 1.5rem;
    cursor: pointer;
  }
  .history-list {
    display: grid;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    overflow: hidden;
  }
  .history-list article {
    display: grid;
    grid-template-columns: 1.4fr 1fr 1fr;
    gap: var(--space-4);
    padding: var(--space-4);
    border-bottom: 1px solid var(--border-subtle);
  }
  .history-list article:last-child {
    border: 0;
  }
  .history-list article div {
    display: grid;
    gap: 0.2rem;
  }
  .history-dialog footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-4);
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
    margin-bottom: var(--space-4);
  }
  .mode-switch {
    display: flex;
    width: max-content;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: 0.25rem;
  }
  .mode-switch button {
    min-height: 2.25rem;
    border: 0;
    border-radius: var(--radius-sm);
    background: transparent;
    padding: 0 var(--space-4);
    color: var(--text-secondary);
    font-weight: 700;
    cursor: pointer;
  }
  .mode-switch button.active {
    background: var(--color-brand-50);
    color: var(--color-brand-700);
  }
  .observations {
    display: grid;
    width: 100%;
    gap: var(--space-2);
    margin-top: var(--space-4);
    color: var(--text-primary);
    font-size: var(--text-sm);
    font-weight: 650;
  }
  textarea {
    width: 100%;
    min-height: 5rem;
    resize: vertical;
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: var(--space-3);
    color: var(--text-primary);
    font: inherit;
  }
  .checks {
    display: grid;
    gap: var(--space-3);
  }
  .checks label {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    font-weight: 600;
  }
  .checks input {
    width: 1.1rem;
    min-height: 1.1rem;
    accent-color: var(--color-brand-500);
  }
  .empty {
    padding: var(--space-5);
    color: var(--text-secondary);
    font-size: var(--text-sm);
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
    :global(.agenda article .badge),
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
