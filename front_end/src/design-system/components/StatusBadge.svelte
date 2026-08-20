<script lang="ts">
  let { status }: { status: 'confirmed' | 'pending' | 'completed' | 'cancelled' | 'missed' } = $props();
  const labels = { confirmed: 'Confirmado', pending: 'Pendente', completed: 'Concluído', cancelled: 'Cancelado', missed: 'Não compareceu' };
</script>

<span class="badge {status}">{labels[status]}</span>

<style>
  .badge { display:inline-flex; align-items:center; border-radius:var(--radius-pill); padding:.3rem .65rem; font-size:var(--text-xs); font-weight:700; }
  .confirmed, .completed { color:var(--status-success); background:var(--status-success-bg); }
  .pending { color:var(--status-warning); background:var(--status-warning-bg); }
  .cancelled, .missed { color:var(--status-danger); background:var(--status-danger-bg); }
</style>
