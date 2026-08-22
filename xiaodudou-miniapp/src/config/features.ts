export function resolveAiFeatureEnabled(isDevelopment: boolean, rawFlag?: string) {
  return isDevelopment && rawFlag === 'true'
}

const buildEnv = (import.meta as ImportMeta & { env?: Record<string, string | boolean | undefined> }).env
export const AI_FEATURE_ENABLED = resolveAiFeatureEnabled(
  buildEnv?.DEV === true,
  typeof buildEnv?.VITE_AI_FEATURE_ENABLED === 'string' ? buildEnv.VITE_AI_FEATURE_ENABLED : undefined
)
