<script lang="ts">
  let {
    status,
  }: {
    status: 'confirmed' | 'pending' | 'waiting' | 'in_service' | 'completed' | 'cancelled' | 'canceled' | 'missed';
  } = $props();
  const labels = {
    confirmed: 'Confirmado',
    pending: 'Pendente',
    waiting: 'Na espera',
    in_service: 'Em atendimento',
    completed: 'Concluído',
    cancelled: 'Cancelado',
    canceled: 'Cancelado',
    missed: 'Não compareceu',
  };
</script>

<span class="badge {status}">{labels[status]}</span>

<style>
  .badge {
    display: inline-flex;
    align-items: center;
    border-radius: var(--radius-pill);
    padding: 0.3rem 0.65rem;
    font-size: var(--text-xs);
    font-weight: 700;
  }
  .confirmed,
  .completed {
    color: var(--status-success);
    background: var(--status-success-bg);
  }
  .pending,
  .waiting {
    color: var(--status-warning);
    background: var(--status-warning-bg);
  }
  .in_service {
    color: var(--color-brand-700);
    background: var(--color-brand-50);
  }
  .cancelled,
  .canceled,
  .missed {
    color: var(--status-danger);
    background: var(--status-danger-bg);
  }
</style>
