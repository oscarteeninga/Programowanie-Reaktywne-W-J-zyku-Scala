package EShop.lab2.simple

import akka.actor.ActorRef

trait Command

// Common
case object StartCheckout            extends Command
case object ConfirmCheckoutCancelled extends Command
case object ConfirmCheckoutClosed    extends Command

// to Cart
case class AddItem(item: Any)                    extends Command
case class RemoveItem(item: Any)                 extends Command
case object ExpireCart                           extends Command
case class StartWithCheckout(actorRef: ActorRef) extends Command

// to Checkout
case object StartTestCheckout                   extends Command
case class SelectDeliveryMethod(method: String) extends Command
case object CancelCheckout                      extends Command
case object ExpireCheckout                      extends Command
case class SelectPayment(payment: String)       extends Command
case object ExpirePayment                       extends Command
case object ConfirmPaymentReceived              extends Command
