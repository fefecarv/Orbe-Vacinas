export type UserRole = 'patient' | 'employee' | 'admin';

export const pagePaths: Record<string, string> = {
  home: '/paciente/inicio',
  appointments: '/paciente/agendamentos',
  booking: '/paciente/agendamentos/novo',
  vaccines: '/paciente/vacinas',
  history: '/paciente/carteira-vacinal',
  family: '/paciente/dependentes',
  profile: '/paciente/dados-cadastrais',
  insurance: '/paciente/convenios',
  security: '/seguranca',
  help: '/ajuda',
  'staff-dashboard': '/funcionario/inicio',
  'staff-agenda': '/funcionario/agenda',
  'staff-patients': '/funcionario/pacientes',
  'staff-application': '/funcionario/aplicacoes/nova',
  'admin-dashboard': '/admin/dashboard',
  'admin-users': '/admin/usuarios',
  'admin-vaccines': '/admin/vacinas',
  'admin-stock': '/admin/estoque',
  'admin-insurance': '/admin/convenios',
  'admin-schedule': '/admin/funcionamento',
  'admin-reports': '/admin/relatorios',
  'admin-audit': '/admin/auditoria',
  'not-found': '/404',
  'access-denied': '/acesso-negado'
};

export const renderedPageIds = [
  'home', 'appointments', 'booking', 'vaccines', 'history', 'family', 'profile', 'insurance',
  'security', 'help', 'staff-dashboard', 'staff-agenda', 'staff-patients', 'staff-application',
  'admin-dashboard', 'admin-users', 'admin-vaccines', 'admin-stock', 'admin-insurance', 'admin-schedule',
  'admin-reports', 'admin-audit', 'not-found', 'access-denied'
] as const;

const pathPages = Object.fromEntries(Object.entries(pagePaths).map(([page, path]) => [path, page]));

export function pageFromPath(pathname: string): string {
  return pathPages[pathname] ?? 'not-found';
}

export function roleFromPath(pathname: string): UserRole {
  if (pathname.startsWith('/admin')) return 'admin';
  if (pathname.startsWith('/funcionario')) return 'employee';
  return 'patient';
}

export function pathAllowedForRole(pathname: string, role: UserRole): boolean {
  if (pathname.startsWith('/admin')) return role === 'admin';
  if (pathname.startsWith('/funcionario')) return role === 'employee';
  if (pathname.startsWith('/paciente')) return role === 'patient';
  return true;
}

export function isKnownPage(page: string): boolean {
  return Object.hasOwn(pagePaths, page);
}
