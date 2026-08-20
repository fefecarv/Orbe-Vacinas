export type QueueStatus = 'confirmed' | 'waiting' | 'in_service' | 'completed' | 'canceled' | 'missed';

const statusMap: Record<string, QueueStatus> = {
  PENDENTE: 'confirmed',
  CONFIRMADO: 'confirmed',
  ESPERA: 'waiting',
  EM_ATENDIMENTO: 'in_service',
  CONCLUIDO: 'completed',
  CANCELADO: 'canceled',
  FALTOU: 'missed',
};

export function mapEmployeeStatus(status: string): QueueStatus {
  return statusMap[status] ?? 'missed';
}

export function localDateValue(date = new Date()): string {
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 10);
}

export function canAdvanceStatus(status: QueueStatus): boolean {
  return status === 'confirmed' || status === 'waiting' || status === 'in_service';
}
