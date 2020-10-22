package EShop.lab2.simple

trait Command

// Common
case object StartCheckout extends Command

// to Cart
case class AddItem(item: Any)    extends Command
case class RemoveItem(item: Any) extends Command
case object ExpireCart           extends Command

// to Checkout
case class SelectDeliveryMethod(method: String) extends Command
case object CancelCheckout                      extends Command
case object ExpireCheckout                      extends Command
case class SelectPayment(payment: String)       extends Command
case object ExpirePayment                       extends Command
