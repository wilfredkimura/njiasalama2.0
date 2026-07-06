package com.njiasalama.domain.model

enum class RouteSafetyStatus {
    SAFE,       // 0 hazards -> Triggers green MD3 success banner + disclaimer Toast
    CAUTION,    // 1-3 hazards
    DANGEROUS   // >3 hazards
}
