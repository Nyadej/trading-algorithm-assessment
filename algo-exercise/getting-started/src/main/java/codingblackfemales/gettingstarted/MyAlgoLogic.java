package codingblackfemales.gettingstarted;

import codingblackfemales.action.*;
import codingblackfemales.action.Action;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This algorithm uses Volume Weighted Average Price (VWAP) to decide whether to buy, sell, cancel, or hold shares.
 *
 * Strategy:
 * - BUY when the current price is below the VWAP and the number of active buy orders is below a desired limit.
 * - SELL when the current price is above the VWAP and there are active buy orders.
 * - CANCEL orders when the VWAP indicates extreme market conditions to manage risk.
 * - HOLD when none of the above conditions are met.
 *
 * VWAP Calculation:
 *  VWAP = (Sum of (Price * Quantity) for all orders) / (Sum of Quantity for all orders)
 * - Using the top 3 bid and ask levels to approximate the VWAP due to the absence of historical trade data.
 * - This approach gives an estimate of market value based on the order book, providing an estimate of its liquidity.
 * - Helps to make decisions without relying on past trading data.
 * - If no active orders are present, a default VWAP is used.
 *
 * Risk Management:
 * - Limits the total number of orders to prevent overexposure.
 * - Cancels orders if VWAP is outside the acceptable range (too low or too high).
 *
 */

public class MyAlgoLogic implements AlgoLogic {

    // Tracking the state of the portfolio
    private long sharesOwned = 1000L; // Setting initial sharesOwned to 1000
    private long totalSpent = 0L; // tack the total money spent on buying shares
    private long totalEarned = 0L; // track the total money earned from selling shares
    private long profit = 0L; // tracks the overall profit/loss

    // Defining possible trade actions
    enum TradeAction {
        BUY, SELL, CANCEL, HOLD
    }

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);

    // Evaluate is called to determine the appropriate action based on market conditions
    @Override
    public Action evaluate(SimpleAlgoState state) {

        TradeAction action; // declaring a variable action that will hold the decisions (BUY,SELL,HOLD) made by the algo

        /* Retrieve the top bid level information from the market data
        1. */
        BidLevel level = state.getBidAt(0);
        final long price = level.price;
        final long quantity = level.quantity;

        // CONSTANTS FOR DESIRED AND TOTAL ORDER LIMITS
        final int DESIRED_ACTIVE_ORDERS = 3;
        final int TOTAL_ORDER_LIMIT = 6;

        // Get active and total orders from the current state
        final var activeOrders = state.getActiveChildOrders(); // Currently active (unfilled or un-cancelled) orders
        final var totalOrders = state.getChildOrders().size(); // Total number of orders, active or inactive

        double totalMarketValue = 0.0; // store the total number of all active orders
        long totalQuantity = 0; // Stores the total number of shares (quantity) from all active orders

        // Defines the number of bid and ask levels to be used for VWAP calculation
        int orderBookLevels = 3; // using all 3 bid and asks levels on both order book sides

        // Looping through the bid and ask levels to calculate totalMarketValue and totalQuantity
        for (int i = 0; i < orderBookLevels; i++) {
            // Retrieve bid level at index i
            BidLevel bidLevel = state.getBidAt(i);
            if (bidLevel !=null) { // Ensure the bid level exists
                totalMarketValue += bidLevel.price * bidLevel.quantity; // Add (price * quantity) to totalMarketValue
                totalQuantity += bidLevel.quantity; // Add quantity to totalQuantity
        }

            // Retrieve ask level at index i
            AskLevel askLevel = state.getAskAt(i);
            if (askLevel != null) { // Ensure the bid level exists
                totalMarketValue += askLevel.price * askLevel.quantity; // Add (price * quantity) to totalMarketValue
                totalQuantity += askLevel.quantity; // Add quantity to totalQuantity
            }
        }

        // Calculating VWAP (Volume Weighted Average Price)
        double vWAP;
        if (totalQuantity <= 0) { // If there are no active orders
            vWAP = 90; // Set a default VWAP value
            logger.info("No active orders, using hardcoded initial VWAP: {}", vWAP);

            // if there are active order, calculate VWAP
        } else {
            vWAP = totalMarketValue / (double) totalQuantity;
            logger.info("Current total market value = {}", totalMarketValue);
            logger.info("Current total quantity = {}", totalQuantity);
        }

        logger.info("Current VWAP value = {}", vWAP); // Log the calculated VWAP

        // To clarify decision-making
        logger.info("Checking if price: {} < VWAP: {}", price, vWAP);

        // Check if the total number of orders has reached or exceeded the limit
        if (totalOrders >= TOTAL_ORDER_LIMIT) {
            logger.info("[DYNAMIC-PASSIVE-ALGO] Total order limit reached. No new orders will be created.");
            logFinalState(); // Log the final state of the portfolio
            return NoAction.NoAction;
        }

        // Decision-making based on price and VWAP
        // If the price is below VWAP and there are fewer than desired active orders, BUY
        if (price < vWAP && state.getActiveChildOrders().size() < DESIRED_ACTIVE_ORDERS) {
            action = TradeAction.BUY;

            // If the price is above VWAP, and there are active orders, and shares are more than 0, SELL
        } else if (price > vWAP && state.getActiveChildOrders().size() <= DESIRED_ACTIVE_ORDERS && sharesOwned > 0) {
            action = TradeAction.SELL;

            // If the cancel condition is met (VWAP too low or too high), CANCEL the oldest active order
        } else if ((vWAP <= 60 || vWAP >= 90) && !state.getActiveChildOrders().isEmpty()) {
                    logger.info("Cancel condition triggered: VWAP is: {} .", vWAP);
                    logger.info("Number of active orders: {}", activeOrders.size());
                    action = TradeAction.CANCEL;

            // Otherwise, HOLD position
                } else {
                    action = TradeAction.HOLD;
                }

                switch (action) {
                    case BUY:
                        logger.info("[DYNAMIC-PASSIVE-ALGO] Price: {} is less than VWAP: {}, buying shares", price, vWAP);
                        sharesOwned += quantity;
                        totalSpent += price * quantity; // because in order books, the price = the price per unit of the quantity being traded
                        logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {}", sharesOwned, totalSpent);
                        return new CreateChildOrder(Side.BUY, quantity, price);

                    case SELL:
                        logger.info("[DYNAMIC-PASSIVE-ALGO] Price: {} is higher than VWAP: {}, selling shares", price, vWAP);

                        // Ensure you own enough shares to sell
                        if (sharesOwned > 0) {
                            sharesOwned -= quantity;
                            totalEarned += price * quantity;

                            // Include market value of remaining shares in profit calculation
                            long marketValueOfSharesOwned = sharesOwned * price;
                            profit = (totalEarned + marketValueOfSharesOwned) - totalSpent;

                            logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {} | Total Earned: {} | Profit: {}", sharesOwned, totalSpent, totalEarned, profit);
                            return new CreateChildOrder(Side.SELL, quantity, price);
                        } else {
                            return new CreateChildOrder(Side.SELL, sharesOwned, price);
                        }

                    case CANCEL:
                        // 1. find the oldest active order
                        final var oldestOrder = activeOrders.stream().findFirst().orElse(null);
                        //var childOrder = oldestOrder.get();

                        // 2. check if the oldest order exists
                        if (oldestOrder != null) {
                            // 3. extract the quantity and price of the oldest order
                            long oldestOrderQuantity = oldestOrder.getQuantity();
                            long oldestOrderPrice = oldestOrder.getPrice();

                            logger.info("Cancelling oldest order: Price: {}, Quantity: {}", oldestOrder.getPrice(), oldestOrder.getQuantity());
                            logger.info("Current VWAP: {} (out of the acceptable range)", vWAP);

                            return new CancelChildOrder(oldestOrder);
                        } else {
                            logger.info("No valid order found to cancel.");
                            return NoAction.NoAction;
                        }

                    default:
                        logger.info("[DYNAMIC-PASSIVE-ALGO] Holding position, no action needed. Share quantity remains: {}.", sharesOwned);
                        return NoAction.NoAction;

                }

            }

            private void logFinalState() {
                logger.info("[FINAL STATE REPORT \uD83C\uDFE6] Shares Owned: {} | Total Spent: {} | Total Earned: {} | Profit: {}", sharesOwned, totalSpent, totalEarned, profit);
            }
        }