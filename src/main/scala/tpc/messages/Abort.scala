package tpc.messages

import tpc.TransactionId

case class Abort(transactionId: TransactionId)
