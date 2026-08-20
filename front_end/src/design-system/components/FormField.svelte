<script lang="ts">
  import type { HTMLInputAttributes } from 'svelte/elements';
  let {
    id,
    label,
    type = 'text',
    value = '',
    placeholder = '',
    autocomplete,
    required = false,
    disabled = false,
    error = '',
    hint = '',
    oninput,
  }: {
    id: string;
    label: string;
    type?: 'text' | 'email' | 'password' | 'tel' | 'date' | 'time';
    value?: string;
    placeholder?: string;
    autocomplete?: HTMLInputAttributes['autocomplete'];
    required?: boolean;
    disabled?: boolean;
    error?: string;
    hint?: string;
    oninput?: (value: string) => void;
  } = $props();
</script>

<div class="field">
  <label for={id}
    >{label}{#if required}<span aria-hidden="true">*</span>{/if}</label
  >
  <input
    {id}
    {type}
    {value}
    {placeholder}
    {autocomplete}
    {required}
    {disabled}
    aria-invalid={error ? 'true' : undefined}
    aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined}
    oninput={(event) => oninput?.(event.currentTarget.value)}
  />
  {#if error}<p id={`${id}-error`} class="error">{error}</p>{:else if hint}<p id={`${id}-hint`} class="hint">
      {hint}
    </p>{/if}
</div>

<style>
  .field {
    display: grid;
    gap: var(--space-2);
  }
  label {
    color: var(--text-primary);
    font-size: var(--text-sm);
    font-weight: 650;
  }
  label span {
    margin-left: 0.2rem;
    color: var(--status-danger);
  }
  input {
    width: 100%;
    min-height: 2.875rem;
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: 0 var(--space-4);
    color: var(--text-primary);
    outline: 0;
    transition:
      border-color var(--transition-fast),
      box-shadow var(--transition-fast),
      background var(--transition-fast);
  }
  input::placeholder {
    color: var(--text-tertiary);
  }
  input:hover:not(:disabled) {
    border-color: var(--text-tertiary);
  }
  input:focus {
    border-color: var(--color-brand-500);
    box-shadow: 0 0 0 3px var(--focus-ring);
  }
  input[aria-invalid='true'] {
    border-color: var(--status-danger);
  }
  input:disabled {
    cursor: not-allowed;
    opacity: 0.55;
    background: var(--surface-subtle);
  }
  .hint,
  .error {
    font-size: var(--text-xs);
    line-height: 1.45;
  }
  .hint {
    color: var(--text-secondary);
  }
  .error {
    color: var(--status-danger);
  }
</style>
