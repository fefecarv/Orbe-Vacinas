<script lang="ts">
  import { onMount } from 'svelte';
  import AppShell from './layout/AppShell.svelte';
  import AuthLayout from './features/auth/AuthLayout.svelte';
  import LoginPage from './features/auth/LoginPage.svelte';
  import RegisterPage from './features/auth/RegisterPage.svelte';
  import ForgotPasswordPage from './features/auth/ForgotPasswordPage.svelte';
  import PatientDashboard from './features/patient/PatientDashboard.svelte';
  import AppointmentsPage from './features/patient/AppointmentsPage.svelte';
  import VaccineCatalogPage from './features/patient/VaccineCatalogPage.svelte';
  import VaccineHistoryPage from './features/patient/VaccineHistoryPage.svelte';
  import AccountPage from './features/patient/AccountPage.svelte';
  import BookingPage from './features/patient/BookingPage.svelte';
  import EmployeePage from './features/employee/EmployeePage.svelte';
  import AdminPage from './features/admin/AdminPage.svelte';
  import AdminSchedulePage from './features/admin/AdminSchedulePage.svelte';
  import PlaceholderPage from './features/shared/PlaceholderPage.svelte';
  import SecurityHelpPage from './features/shared/SecurityHelpPage.svelte';
  import ConfirmDialog from './design-system/components/ConfirmDialog.svelte';
  import { isKnownPage, pageFromPath, pagePaths, pathAllowedForRole, roleFromPath, type UserRole } from './lib/navigation';
  import { authApi, roleFromUser, currentUser } from './lib/api';
  let activePage = $state(pageFromPath(location.pathname));
  let authenticated = $state(false);
  let authChecking = $state(true);
  let authPage = $state<'login' | 'register' | 'forgot'>(location.pathname === '/cadastro' ? 'register' : location.pathname === '/recuperar-senha' ? 'forgot' : 'login');
  let bookingVaccine = $state('');
  let role = $state<UserRole>((localStorage.getItem('orbe-session-role') as UserRole | null) ?? roleFromPath(location.pathname));
  let resetDialog = $state(false);
  const titles: Record<string, string> = { vaccines:'Catálogo de vacinas', history:'Carteira vacinal', family:'Minha família', profile:'Dados cadastrais', insurance:'Meus convênios', security:'Segurança', help:'Central de ajuda' };

  function navigate(page: string, replace = false) {
    const targetPage = isKnownPage(page) ? page : 'not-found';
    activePage = targetPage;
    const path = pagePaths[targetPage];
    history[replace ? 'replaceState' : 'pushState']({}, '', path);
  }

  function navigateAuth(page: 'login' | 'register' | 'forgot') {
    authPage = page;
    history.pushState({}, '', page === 'register' ? '/cadastro' : page === 'forgot' ? '/recuperar-senha' : '/login');
  }

  function login(selectedRole: UserRole) {
    role = selectedRole;
    authenticated = true;
    localStorage.setItem('orbe-session-role', selectedRole);
    navigate(currentUser()?.trocaSenhaObrigatoria ? 'security' : selectedRole === 'admin' ? 'admin-dashboard' : selectedRole === 'employee' ? 'staff-dashboard' : 'home', true);
  }

  async function logout() {
    await authApi.logout().catch(() => undefined);
    authenticated = false;
    role = 'patient';
    activePage = 'home';
    localStorage.removeItem('orbe-session-role');
    history.replaceState({}, '', '/login');
  }

  function resetDemo() {
    const preservedTheme = localStorage.getItem('orbe-theme');
    const preservedRole = localStorage.getItem('orbe-session-role');
    Object.keys(localStorage).filter(key => key.startsWith('orbe-')).forEach(key => localStorage.removeItem(key));
    if (preservedTheme) localStorage.setItem('orbe-theme', preservedTheme);
    if (preservedRole) localStorage.setItem('orbe-session-role', preservedRole);
    resetDialog = false;
    location.reload();
  }

  onMount(() => {
    const handlePopState = () => {
      if (authenticated) activePage = pathAllowedForRole(location.pathname, role) ? pageFromPath(location.pathname) : 'access-denied';
      else authPage = location.pathname === '/cadastro' ? 'register' : location.pathname === '/recuperar-senha' ? 'forgot' : 'login';
    };
    addEventListener('popstate', handlePopState);
    void (async () => {
      try {
        const user = await authApi.current();
        role = roleFromUser(user);
        authenticated = true;
        localStorage.setItem('orbe-session-role', role);
        activePage = pathAllowedForRole(location.pathname, role)
          ? pageFromPath(location.pathname)
          : 'access-denied';
      } catch {
        authenticated = false;
        localStorage.removeItem('orbe-session-role');
        if (!['/login', '/cadastro', '/recuperar-senha'].includes(location.pathname)) {
          history.replaceState({}, '', '/login');
        }
      } finally {
        authChecking = false;
      }
    })();
    return () => removeEventListener('popstate', handlePopState);
  });
</script>

{#if authChecking}
  <div class="session-loading" role="status">Carregando...</div>
{:else if authenticated}
  <AppShell {activePage} {role} onNavigate={navigate} onLogout={logout} onReset={() => resetDialog = true}>
    {#if activePage === 'home'}<PatientDashboard onNavigate={navigate} />
    {:else if activePage === 'appointments'}<AppointmentsPage onSchedule={() => { bookingVaccine = ''; navigate('booking'); }} />
    {:else if activePage === 'booking'}<BookingPage initialVaccine={bookingVaccine} onFinish={() => navigate('appointments')} onCancel={() => navigate('appointments')} />
    {:else if activePage === 'vaccines'}<VaccineCatalogPage onSchedule={(id) => { bookingVaccine = id; navigate('booking'); }} />
    {:else if activePage === 'history'}<VaccineHistoryPage onNavigate={navigate} />
    {:else if activePage === 'family'}<AccountPage mode="family" />
    {:else if activePage === 'insurance'}<AccountPage mode="insurance" />
    {:else if activePage === 'profile'}<AccountPage mode="profile" />
    {:else if activePage === 'staff-dashboard'}<EmployeePage mode="dashboard" onNavigate={navigate}/>
    {:else if activePage === 'staff-agenda'}<EmployeePage mode="agenda" onNavigate={navigate}/>
    {:else if activePage === 'staff-patients'}<EmployeePage mode="patients" onNavigate={navigate}/>
    {:else if activePage === 'staff-application'}<EmployeePage mode="application" onNavigate={navigate}/>
    {:else if activePage === 'admin-dashboard'}<AdminPage mode="dashboard" onNavigate={navigate}/>
    {:else if activePage === 'admin-users'}<AdminPage mode="users" onNavigate={navigate}/>
    {:else if activePage === 'admin-vaccines'}<AdminPage mode="vaccines" onNavigate={navigate}/>
    {:else if activePage === 'admin-stock'}<AdminPage mode="stock" onNavigate={navigate}/>
    {:else if activePage === 'admin-insurance'}<AdminPage mode="insurance" onNavigate={navigate}/>
    {:else if activePage === 'admin-schedule'}<AdminSchedulePage />
    {:else if activePage === 'admin-reports'}<AdminPage mode="reports" onNavigate={navigate}/>
    {:else if activePage === 'admin-audit'}<AdminPage mode="audit" onNavigate={navigate}/>
    {:else if activePage === 'security'}<SecurityHelpPage mode="security" />
    {:else if activePage === 'help'}<SecurityHelpPage mode="help" />
    {:else if activePage === 'not-found'}<PlaceholderPage title="Página não encontrada" description="O endereço informado não existe ou foi alterado." onBack={() => navigate(role === 'admin' ? 'admin-dashboard' : role === 'employee' ? 'staff-dashboard' : 'home')} />
    {:else if activePage === 'access-denied'}<PlaceholderPage title="Acesso não autorizado" description="Seu perfil não possui permissão para acessar esta área." onBack={() => navigate(role === 'admin' ? 'admin-dashboard' : role === 'employee' ? 'staff-dashboard' : 'home')} />
    {:else}<PlaceholderPage title={titles[activePage] ?? 'Orbe'} onBack={() => navigate(role === 'admin' ? 'admin-dashboard' : role === 'employee' ? 'staff-dashboard' : 'home')} />{/if}
  </AppShell>
  {#if resetDialog}<ConfirmDialog title="Restaurar dados de demonstração?" description="Cadastros, aplicações, fila e alterações locais voltarão ao estado inicial. Tema e sessão serão preservados." confirmLabel="Restaurar dados" danger onConfirm={resetDemo} onCancel={() => resetDialog = false}/>{/if}
{:else}
  <AuthLayout>
    {#if authPage === 'login'}
      <LoginPage onLogin={login} onNavigate={navigateAuth} />
    {:else if authPage === 'register'}
      <RegisterPage onBack={() => navigateAuth('login')} />
    {:else}
      <ForgotPasswordPage onBack={() => navigateAuth('login')} />
    {/if}
  </AuthLayout>
{/if}

<style>
  .session-loading {
    display: grid;
    min-height: 100dvh;
    place-items: center;
    background: var(--surface-page);
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
</style>
