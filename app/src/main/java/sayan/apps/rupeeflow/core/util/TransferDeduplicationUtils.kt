package sayan.apps.rupeeflow.core.util

import sayan.apps.rupeeflow.domain.model.Transaction

fun List<Transaction>.deduplicateTransfers(): List<Transaction> {
    val result = mutableListOf<Transaction>()
    val seenTransferIds = mutableSetOf<String>()
    
    val transferGroups = this.filter { it.transferId != null }.groupBy { it.transferId!! }
    
    val canonicalTransfers = transferGroups.mapValues { (_, list) ->
        list.minWithOrNull(
            compareBy<Transaction> { if (it.isIncoming) 1 else 0 }
                .thenBy { it.id.toLongOrNull() ?: 0L }
        ) ?: list.first()
    }
    
    for (transaction in this) {
        val transferId = transaction.transferId
        if (transferId != null) {
            if (transferId !in seenTransferIds) {
                seenTransferIds.add(transferId)
                val canonical = canonicalTransfers[transferId]
                if (canonical != null) {
                    result.add(canonical)
                }
            }
        } else {
            result.add(transaction)
        }
    }
    return result
}
