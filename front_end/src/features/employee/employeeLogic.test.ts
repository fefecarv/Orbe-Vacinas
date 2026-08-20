import { describe, expect, it } from 'vitest';
import { canAdvanceStatus, localDateValue, mapEmployeeStatus } from './employeeLogic';

describe('employee agenda rules', () => {
  it.each([
    ['PENDENTE', 'confirmed'],
    ['CONFIRMADO', 'confirmed'],
    ['ESPERA', 'waiting'],
    ['EM_ATENDIMENTO', 'in_service'],
    ['CONCLUIDO', 'completed'],
    ['CANCELADO', 'canceled'],
    ['FALTOU', 'missed'],
  ])('maps %s correctly', (source, expected) => expect(mapEmployeeStatus(source)).toBe(expected));
  it('never presents an unknown status as confirmed', () => expect(mapEmployeeStatus('UNKNOWN')).toBe('missed'));
  it('blocks terminal status actions', () => {
    expect(canAdvanceStatus('completed')).toBe(false);
    expect(canAdvanceStatus('canceled')).toBe(false);
    expect(canAdvanceStatus('missed')).toBe(false);
  });
  it('uses the local calendar date', () => expect(localDateValue(new Date(2026, 7, 20, 23, 30))).toBe('2026-08-20'));
});
