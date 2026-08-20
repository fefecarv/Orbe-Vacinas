<script lang="ts">
  import type { Snippet } from 'svelte';
  import { applyTheme, getThemePreference, type ThemePreference } from '../design-system/theme/theme';
  import { currentUser } from '../lib/api';

  let {
    children,
    activePage,
    onNavigate,
    onLogout,
    role = 'patient',
  }: {
    children: Snippet;
    activePage: string;
    onNavigate: (page: string) => void;
    onLogout: () => void;
    role?: 'patient' | 'employee' | 'admin';
  } = $props();
  let menuOpen = $state(false);
  let theme = $state<ThemePreference>('system');
  const sessionUser = currentUser();

  const patientNavGroups = [
    {
      label: '',
      items: [
        { id: 'home', icon: '⌂', label: 'Início' },
        { id: 'appointments', icon: '✦', label: 'Agendamentos' },
        { id: 'vaccines', icon: '✚', label: 'Vacinas' },
        { id: 'history', icon: '▤', label: 'Carteira vacinal' },
      ],
    },
    {
      label: 'Minha conta',
      items: [
        { id: 'family', icon: '♧', label: 'Minha família' },
        { id: 'profile', icon: '○', label: 'Dados cadastrais' },
        { id: 'insurance', icon: '◇', label: 'Convênios' },
      ],
    },
    {
      label: 'Geral',
      items: [
        { id: 'security', icon: '▣', label: 'Segurança' },
        { id: 'help', icon: '?', label: 'Ajuda' },
      ],
    },
  ];
  const employeeNavGroups = [
    {
      label: '',
      items: [
        { id: 'staff-dashboard', icon: '⌂', label: 'Visão geral' },
        { id: 'staff-agenda', icon: '▤', label: 'Agenda do dia' },
        { id: 'staff-patients', icon: '♧', label: 'Pacientes' },
        { id: 'staff-application', icon: '✚', label: 'Registrar aplicação' },
      ],
    },
    {
      label: 'Geral',
      items: [
        { id: 'help', icon: '?', label: 'Ajuda' },
        { id: 'security', icon: '▣', label: 'Segurança' },
      ],
    },
  ];
  const adminNavGroups = [
    {
      label: '',
      items: [
        { id: 'admin-dashboard', icon: '⌂', label: 'Dashboard' },
        { id: 'admin-users', icon: '♧', label: 'Usuários' },
        { id: 'admin-vaccines', icon: '✚', label: 'Vacinas' },
        { id: 'admin-stock', icon: '▤', label: 'Estoque e lotes' },
        { id: 'admin-insurance', icon: '◇', label: 'Convênios' },
        { id: 'admin-schedule', icon: '◷', label: 'Funcionamento' },
      ],
    },
    {
      label: 'Gestão',
      items: [
        { id: 'admin-reports', icon: '◫', label: 'Relatórios' },
        { id: 'admin-audit', icon: '◎', label: 'Auditoria' },
      ],
    },
  ];
  let navGroups = $derived(
    role === 'admin' ? adminNavGroups : role === 'employee' ? employeeNavGroups : patientNavGroups,
  );

  $effect(() => {
    theme = getThemePreference();
  });

  function changeTheme(value: ThemePreference) {
    theme = value;
    applyTheme(value);
  }

  function navigate(page: string) {
    onNavigate(page);
    menuOpen = false;
  }
</script>

<div class="app-shell">
  <button class="mobile-menu" aria-label="Abrir menu" onclick={() => (menuOpen = true)}>☰</button>
  {#if menuOpen}<button class="backdrop" aria-label="Fechar menu" onclick={() => (menuOpen = false)}></button>{/if}
  <aside class:open={menuOpen}>
    <div class="brand" aria-label="Orbe"><span>or</span>be<i></i></div>
    <p class="welcome">
      Olá, {sessionUser?.nome.split(' ')[0] ?? 'usuário'}
    </p>
    <nav aria-label="Navegação principal">
      {#each navGroups as group}
        {#if group.label}<p class="group-label">{group.label}</p>{/if}
        {#each group.items as item}
          <button class:active={activePage === item.id} onclick={() => navigate(item.id)}>
            <span class="nav-icon" aria-hidden="true">{item.icon}</span>{item.label}
          </button>
        {/each}
      {/each}
    </nav>
    <div class="sidebar-footer">
      <label for="theme">Tema</label>
      <select id="theme" value={theme} onchange={(event) => changeTheme(event.currentTarget.value as ThemePreference)}>
        <option value="system">Do dispositivo</option>
        <option value="light">Claro</option>
        <option value="dark">Escuro</option>
      </select>
      <button class="logout" onclick={onLogout}><span aria-hidden="true">↪</span> Sair</button>
    </div>
  </aside>
  <main>{@render children()}</main>
</div>

<style>
  .app-shell {
    min-height: 100vh;
  }
  aside {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 20;
    display: flex;
    width: var(--sidebar-width);
    height: 100dvh;
    flex-direction: column;
    overflow: hidden;
    border-right: 1px solid var(--border-subtle);
    background: var(--surface-sidebar);
    padding: var(--space-5) var(--space-4);
  }
  .brand {
    color: var(--color-brand-500);
    font-size: 1.65rem;
    font-weight: 800;
    letter-spacing: -0.09em;
  }
  .brand span {
    font-weight: 500;
  }
  .brand i {
    display: inline-block;
    width: 0.35rem;
    height: 0.35rem;
    margin-left: 0.15rem;
    border-radius: 50%;
    background: var(--color-accent-400);
    vertical-align: top;
  }
  .welcome {
    margin: var(--space-4) var(--space-2) var(--space-3);
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }
  nav {
    display: grid;
    gap: var(--space-1);
  }
  nav button,
  .logout {
    display: flex;
    width: 100%;
    min-height: 2.4rem;
    align-items: center;
    gap: var(--space-3);
    border: 0;
    border-radius: var(--radius-sm);
    background: transparent;
    padding: 0.5rem var(--space-3);
    color: var(--text-secondary);
    font-size: var(--text-sm);
    text-align: left;
    cursor: pointer;
    transition:
      background var(--transition-fast),
      color var(--transition-fast);
  }
  nav button:hover,
  .logout:hover {
    background: var(--surface-hover);
    color: var(--text-primary);
  }
  nav button.active {
    background: var(--color-brand-50);
    color: var(--color-brand-600);
    font-weight: 700;
  }
  :global([data-theme='dark']) nav button.active {
    background: rgb(17 124 111 / 0.2);
    color: #70c9bd;
  }
  .nav-icon {
    display: grid;
    width: 1.25rem;
    place-items: center;
    font-size: 1rem;
    font-weight: 700;
  }
  .group-label {
    margin: var(--space-3) var(--space-3) var(--space-1);
    color: var(--text-tertiary);
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }
  .sidebar-footer {
    display: grid;
    gap: var(--space-1);
    margin-top: auto;
    padding-top: var(--space-3);
  }
  .sidebar-footer label {
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
  select {
    width: 100%;
    min-height: 2.25rem;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-sm);
    background: var(--surface-card);
    padding: 0.4rem 0.7rem;
    color: var(--text-primary);
    font-size: var(--text-xs);
  }
  .logout {
    color: var(--color-brand-500);
  }
  main {
    min-height: 100vh;
    margin-left: var(--sidebar-width);
  }
  .mobile-menu,
  .backdrop {
    display: none;
  }
  @media (max-width: 800px) {
    aside {
      transform: translateX(-100%);
      transition: transform var(--transition-fast);
    }
    aside.open {
      transform: translateX(0);
    }
    main {
      margin-left: 0;
      padding-top: 3.75rem;
    }
    .mobile-menu {
      position: fixed;
      z-index: 15;
      display: grid;
      width: 2.65rem;
      height: 2.65rem;
      top: 0.65rem;
      left: 0.75rem;
      place-items: center;
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-sm);
      background: var(--surface-card);
      box-shadow: var(--shadow-sm);
      cursor: pointer;
    }
    .backdrop {
      position: fixed;
      inset: 0;
      z-index: 19;
      display: block;
      border: 0;
      background: var(--surface-overlay);
    }
  }
</style>
