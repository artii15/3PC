package tpc.actors

import java.util.UUID

import akka.actor.Actor
import tpc.{EmptyID, TransactionId, TransactionOperation}
import tpc.actors.states.WorkerState
import tpc.config.WorkerConfig
import tpc.messages._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.duration.DurationInt

class Worker(config: WorkerConfig) extends Actor {
  import WorkerState._

  var currentTransactionId: TransactionId = EmptyID
  val executedOperations: mutable.MutableList[TransactionOperation] = mutable.MutableList()

  override def receive: Receive = {
    case TransactionBeginOrder(transactionId) => beginTransaction(transactionId)
  }

  private def beginTransaction(transactionId: TransactionId): Unit = {
    currentTransactionId = transactionId
    val timeout = WorkerTimeout(currentTransactionId, WAITING_OPERATIONS)
    context.system.scheduler.scheduleOnce(config.getOperationsExecutingTimeout seconds, self, timeout)
    context become executingTransaction
  }

  private def executingTransaction: Receive = {
    case TransactionOperations(operation) => executeOperation(operation)
    //case WorkerTimeout(transactionId, WAITING_OPERATIONS) if Transaction
    case TransactionCommitRequest => waitForCommitDecision()
  }

  private def executeOperation(operation: TransactionOperation): Unit = {
    operation.execute()
    executedOperations += operation
  }

  private def waitForCommitDecision(): Unit = {
    context.parent ! CommitAgree
    context become waitingForCommitDecision
  }

  private def waitingForCommitDecision: Receive = {
    case PrepareToCommit => prepareToCommit()
  }

  private def prepareToCommit(): Unit = {
    context.parent ! CommitAck
    context become waitingForFinalCommit
  }

  private def waitingForFinalCommit: Receive = {
    case CommitConfirmation => doCommit()
  }

  private def doCommit(): Unit = {
    executedOperations.foreach(_.commit())
  }
}
