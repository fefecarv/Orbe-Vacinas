import type { UserRole } from './navigation';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/orbe-backend/api').replace(/\/$/, '');

type ApiEnvelope<T> = {
  sucesso: boolean;
  mensagem: string | null;
  dados: T;
};

export type AuthenticatedUser = {
  id: number;
  nome: string;
  email: string;
  perfis: Array<'PACIENTE' | 'FUNCIONARIO' | 'ADMINISTRADOR'>;
  csrfToken: string;
  trocaSenhaObrigatoria: boolean;
  unidade: string | null;
};

export type PatientRegistration = {
  nome: string;
  cpf: string;
  email: string;
  telefone: string;
  dataNascimento: string;
  senha: string;
};

export type ApiUserProfile = {
  id: number;
  nome: string;
  cpf: string;
  email: string;
  telefone: string;
  dataNascimento: string;
  cep: string | null;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  estado: string | null;
};

let csrfToken = '';
let authenticatedUser: AuthenticatedUser | null = null;

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = new Headers(init.headers);
  if (init.body) headers.set('Content-Type', 'application/json');
  if (!['GET', 'HEAD'].includes(method) && csrfToken) {
    headers.set('X-CSRF-Token', csrfToken);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    method,
    headers,
    credentials: 'include',
  });
  const envelope = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;
  if (!response.ok || !envelope?.sucesso) {
    throw new Error(envelope?.mensagem ?? 'Não foi possível concluir a operação.');
  }
  return envelope.dados;
}

function rememberSession(user: AuthenticatedUser): AuthenticatedUser {
  csrfToken = user.csrfToken;
  authenticatedUser = user;
  return user;
}

export const authApi = {
  async login(email: string, senha: string) {
    return rememberSession(
      await request<AuthenticatedUser>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, senha }),
      }),
    );
  },

  async current() {
    return rememberSession(await request<AuthenticatedUser>('/auth/me'));
  },

  async logout() {
    try {
      await request<null>('/auth/logout', { method: 'POST' });
    } finally {
      csrfToken = '';
      authenticatedUser = null;
    }
  },
  changePassword(senhaAtual: string, novaSenha: string) {
    return request<null>('/auth/alterar-senha', { method: 'POST', body: JSON.stringify({ senhaAtual, novaSenha }) });
  },

  register(data: PatientRegistration) {
    return request('/usuarios', {
      method: 'POST',
      body: JSON.stringify({
        usuario: {
          nome: data.nome,
          cpf: data.cpf.replace(/\D/g, ''),
          email: data.email,
          telefone: data.telefone,
          dataNascimento: data.dataNascimento,
        },
        senha: data.senha,
      }),
    });
  },
};

export type ApiVaccine = {
  id: number;
  nome: string;
  fabricante: string;
  descricao: string;
  categoria: string;
  indicacao: string;
  esquemaDoses: string;
  valorBase: number;
  ativo: boolean;
  idadeMinimaMeses: number;
  idadeMaximaMeses: number | null;
  numeroDoses: number;
  intervaloDias: number | null;
  reforcoMeses: number | null;
};

export type ApiAppointment = {
  id: number;
  protocolo: string;
  usuarioId: number | null;
  dependenteId: number | null;
  vacinaId: number;
  convenioId: number | null;
  dataAgendamento: string;
  unidade: string;
  sala: string;
  dosePrevista: string;
  tipoAtendimento: string;
  valorEstimado: number | null;
  status: string;
  motivoCancelamento: string | null;
};

export type ApiApplication = {
  aplicacaoId: number;
  protocolo: string;
  vacina: string;
  fabricante: string;
  dose: string;
  dataAplicacao: string;
  numeroLote: string;
  localAplicacao: string;
  profissional: string;
};

export type ApiDependent = {
  id: number;
  nome: string;
  dataNascimento: string;
  status: string;
};

export type ApiRecommendation = {
  id: number;
  vacina: string;
  dose: string;
  dataPrevista: string;
  motivo: string;
  status: 'RECOMENDADA' | 'AGENDADA' | 'REVISAR' | 'CONCLUIDA' | 'DESCARTADA';
  agendamentoId: number | null;
};

export type ApiInsurance = {
  id: number;
  convenioId: number;
  nomeConvenio: string;
  plano: string;
  numeroCarteirinha: string;
  titular: string;
  dataValidade: string;
  ativo: boolean;
};
export type AcceptedInsurance = { id: number; nome: string; plano: string; codigoOperacional: string; ativo: boolean };
export type InsuranceAnalysis = {
  valorBase: number;
  tipoCobertura: string;
  percentualDesconto: number;
  valorCoberto: number | null;
  valorPaciente: number | null;
  status: string;
  mensagem: string;
};

export type ApiDailyAppointment = {
  id: number;
  usuarioId: number | null;
  dependenteId: number | null;
  paciente: string;
  cpf: string | null;
  vacina: string;
  vacinaId: number;
  dose: string;
  dataAgendamento: string;
  sala: string;
  status: string;
  tipoAtendimento: 'PARTICULAR' | 'CONVENIO' | 'CAMPANHA';
};

export type ApiBatch = {
  id: number;
  vacinaId: number;
  numeroLote: string;
  dataValidade: string;
  quantidadeAtual: number;
  status: string;
};

export type RegisteredApplication = {
  id: number;
  protocolo: string;
  dose: string;
  dataAplicacao: string;
  localAplicacao: string;
  loteId: number;
};

export type StaffPatient = {
  id: string;
  tipo: 'TITULAR' | 'DEPENDENTE';
  nome: string;
  cpf: string | null;
  dataNascimento: string;
  telefone: string | null;
  email: string | null;
  cep: string | null;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  estado: string | null;
  status: string;
  responsavelId: number | null;
  parentesco: string | null;
  ultimaVacina: string | null;
  ultimaAplicacao: string | null;
};

export type StaffPatientInput = {
  tipo: 'TITULAR' | 'DEPENDENTE';
  nome: string;
  cpf: string;
  dataNascimento: string;
  telefone: string;
  email?: string;
  senhaTemporaria?: string;
  cep?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  status: string;
  responsavelId?: number | null;
  parentesco?: string;
};

export const patientApi = {
  profile() {
    return request<ApiUserProfile>('/auth/perfil');
  },

  saveProfile(data: ApiUserProfile) {
    return request<ApiUserProfile>('/auth/perfil', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  vaccines() {
    return request<ApiVaccine[]>('/vacinas');
  },

  appointments(usuarioId: number) {
    return request<ApiAppointment[]>(`/agendamentos?usuarioId=${usuarioId}`);
  },

  appointmentsForDependent(dependenteId: number) {
    return request<ApiAppointment[]>(`/agendamentos?dependenteId=${dependenteId}`);
  },

  applications(usuarioId: number) {
    return request<ApiApplication[]>(`/aplicacoes?usuarioId=${usuarioId}`);
  },

  applicationsForDependent(dependenteId: number) {
    return request<ApiApplication[]>(`/aplicacoes?dependenteId=${dependenteId}`);
  },

  dependents() {
    return request<ApiDependent[]>('/dependentes');
  },

  recommendations(usuarioId?: number, dependenteId?: number) {
    const query = usuarioId ? `usuarioId=${usuarioId}` : `dependenteId=${dependenteId}`;
    return request<ApiRecommendation[]>(`/recomendacoes?${query}`);
  },

  insurances() {
    return request<ApiInsurance[]>('/convenios');
  },

  acceptedInsurances() {
    return request<AcceptedInsurance[]>('/convenios/aceitos');
  },

  saveInsurance(data: {
    id?: number;
    convenioId: number;
    numeroCarteirinha: string;
    titular: string;
    dataValidade: string;
  }) {
    return request<ApiInsurance>(data.id ? `/convenios/${data.id}` : '/convenios', {
      method: data.id ? 'PUT' : 'POST',
      body: JSON.stringify(data),
    });
  },

  analyzeInsurance(vaccineId: number, insuranceId?: number) {
    const convenio = insuranceId ? `&convenioId=${insuranceId}` : '';
    return request<InsuranceAnalysis>(`/agendamentos/analise-convenio?vacinaId=${vaccineId}${convenio}`);
  },

  createAppointment(data: {
    usuarioId: number | null;
    dependenteId: number | null;
    vacinaId: number;
    convenioId: number | null;
    dataAgendamento: string;
    unidade: string;
    sala: string;
    dosePrevista: string;
    tipoAtendimento: 'PARTICULAR' | 'CONVENIO';
    valorEstimado: number | null;
  }) {
    return request<ApiAppointment>('/agendamentos', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  cancelAppointment(id: number, motivo: string) {
    return request<ApiAppointment>(`/agendamentos/${id}`, {
      method: 'DELETE',
      body: JSON.stringify({ motivo }),
    });
  },

  rescheduleAppointment(id: number, novaData: string) {
    return request<ApiAppointment>(`/agendamentos/${id}/reagendar`, {
      method: 'PUT',
      body: JSON.stringify({ novaData }),
    });
  },
  availableTimes(data: string, unidade = 'Orbe Centro') {
    return request<string[]>(`/agendamentos/horarios?data=${data}&unidade=${encodeURIComponent(unidade)}`);
  },
};

export function currentUser() {
  return authenticatedUser;
}

export const staffApi = {
  dailyAgenda(date: string, unidade?: string) {
    const unit = unidade ? `&unidade=${encodeURIComponent(unidade)}` : '';
    return request<ApiDailyAppointment[]>(`/agendamentos?data=${date}${unit}`);
  },

  updateAppointmentStatus(id: number, status: 'ESPERA' | 'EM_ATENDIMENTO' | 'CONCLUIDO') {
    return request<ApiAppointment>(`/agendamentos/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    });
  },

  batches(vaccineId: number) {
    return request<ApiBatch[]>(`/lotes?vacinaId=${vaccineId}`);
  },

  registerApplication(data: {
    agendamentoId: number | null;
    usuarioId: number | null;
    dependenteId: number | null;
    funcionarioId: number;
    loteId: number;
    dose: string;
    dataAplicacao: string;
    tipoAtendimento: 'PARTICULAR' | 'CONVENIO' | 'CAMPANHA';
    viaAdministracao: string;
    localAplicacao: string;
    valorPago: number | null;
    observacoes: string | null;
  }) {
    return request<RegisteredApplication>('/aplicacoes', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  patients() {
    return request<StaffPatient[]>('/pacientes');
  },

  patientHistory(id: string) {
    const [type, value] = id.split(':');
    return request<ApiApplication[]>(`/aplicacoes?${type === 'U' ? 'usuarioId' : 'dependenteId'}=${value}`);
  },

  vaccines() {
    return request<ApiVaccine[]>('/vacinas');
  },

  savePatient(data: StaffPatientInput, id?: string) {
    return request<StaffPatient>(id ? `/pacientes/${id}` : '/pacientes', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(data),
    });
  },
};

export type ApiLot = {
  id: number;
  vacinaId: number;
  numeroLote: string;
  dataValidade: string;
  quantidadeInicial: number;
  quantidadeAtual: number;
  fornecedor: string;
  status: string;
};
export type ApiAdminUser = {
  id: number;
  nome: string;
  cpf: string;
  email: string;
  telefone: string;
  dataNascimento: string;
  status: string;
  perfil: 'PACIENTE' | 'FUNCIONARIO' | 'ADMINISTRADOR';
  matricula: string | null;
  ultimoAcessoEm: string | null;
};
export type ApiAdminInsurance = {
  id: number;
  nome: string;
  plano: string;
  codigoOperacional: string;
  ativo: boolean;
  tipoCobertura: string;
  percentualDesconto: number | null;
  valorCoparticipacao: number | null;
};
export type ManagementReport = {
  pacientesAtivos: number;
  aplicacoesPeriodo: number;
  dosesEstoque: number;
  lotesAlerta: number;
  agendamentosTotal: number;
  concluidos: number;
  faltas: number;
  cancelados: number;
  dosesPerdidas: number;
  receita: number;
  aplicacoesPorSemana: Array<{ periodo: string; quantidade: number }>;
  vacinasMaisAplicadas: Array<{ vacina: string; quantidade: number }>;
  profissionaisMaisAtivos: Array<{ vacina: string; quantidade: number }>;
  alertas: Array<{ lote: string; vacina: string; tipo: string; quantidade: number; validade: string }>;
};

export const adminApi = {
  users: () => request<ApiAdminUser[]>('/admin/usuarios'),
  vaccines: () => request<ApiVaccine[]>('/admin/vacinas'),
  lots: () => request<ApiLot[]>('/admin/lotes'),
  insurances: () => request<ApiAdminInsurance[]>('/admin/convenios'),
  movements: () => request<Record<string, unknown>[]>('/admin/movimentacoes'),
  audit: () => request<Record<string, unknown>[]>('/admin/auditoria'),
  saveVaccine: (data: Partial<ApiVaccine>) =>
    request<ApiVaccine>('/vacinas', { method: data.id ? 'PUT' : 'POST', body: JSON.stringify(data) }),
  saveLot: (data: Partial<ApiLot>) =>
    request<ApiLot>(data.id ? `/admin/lotes/${data.id}` : '/admin/lotes', {
      method: data.id ? 'PUT' : 'POST',
      body: JSON.stringify(data),
    }),
  saveInsurance: (data: Partial<ApiAdminInsurance>) =>
    request<ApiAdminInsurance>(data.id ? `/admin/convenios/${data.id}` : '/admin/convenios', {
      method: data.id ? 'PUT' : 'POST',
      body: JSON.stringify(data),
    }),
  createUser: (data: {
    nome: string;
    cpf: string;
    email: string;
    telefone: string;
    dataNascimento: string;
    senha: string;
    perfil: string;
    matricula?: string;
    unidade?: string;
    trocaSenhaObrigatoria?: boolean;
  }) =>
    request('/usuarios', {
      method: 'POST',
      body: JSON.stringify({
        usuario: {
          nome: data.nome,
          cpf: data.cpf.replace(/\D/g, ''),
          email: data.email,
          telefone: data.telefone,
          dataNascimento: data.dataNascimento,
          unidade: data.unidade,
          trocaSenhaObrigatoria: data.trocaSenhaObrigatoria,
        },
        perfil: {
          perfil: data.perfil,
          matricula: data.matricula,
          cargo: data.perfil === 'FUNCIONARIO' ? 'Funcionário' : 'Administrador',
        },
        senha: data.senha,
      }),
    }),
  report: (inicio: string, fim: string) => request<ManagementReport>(`/admin/relatorio?inicio=${inicio}&fim=${fim}`),
  schedule: () =>
    request<
      Array<{
        id: number;
        unidade: string;
        diaSemana: number;
        horaAbertura: string;
        horaFechamento: string;
        intervaloMinutos: number;
        ativo: boolean;
      }>
    >('/admin/horarios'),
  saveSchedule: (data: Record<string, unknown>) =>
    request('/admin/horarios', { method: 'POST', body: JSON.stringify(data) }),
  blocks: () =>
    request<
      Array<{
        id: number;
        unidade: string;
        dataBloqueio: string;
        horaInicio: string | null;
        horaFim: string | null;
        motivo: string;
      }>
    >('/admin/bloqueios'),
  saveBlock: (data: Record<string, unknown>) =>
    request('/admin/bloqueios', { method: 'POST', body: JSON.stringify(data) }),
};

export function roleFromUser(user: AuthenticatedUser): UserRole {
  if (user.perfis.includes('ADMINISTRADOR')) return 'admin';
  if (user.perfis.includes('FUNCIONARIO')) return 'employee';
  return 'patient';
}
