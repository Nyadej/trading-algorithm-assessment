package codingblackfemales.gettingstarted;

import codingblackfemales.action.*;
import codingblackfemales.action.Action;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
 * - Helps to make decisions by striking a balance between capturing market trends and managing available data
 * - If no active orders are present, a default VWAP is used.
 *
 * Risk Management:
 * - Limits the total number of orders to prevent overexposure.
 * - Cancels orders if VWAP is outside the acceptable range (too low or too high).
 * - Cancel the oldest active order - helping to prevent holding onto positions that no longer align with the market
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

        // Retrieve the top bid level information from the market data
        BidLevel level = state.getBidAt(0); // Get the top bid (price + quantity) in the market
        final long price = level.price;// Price of the top bid
        final long quantity = level.quantity; // Quantity available at the top bid price

        // CONSTANTS FOR DESIRED AND TOTAL ORDER LIMITS
        final int DESIRED_ACTIVE_ORDERS = 3;
        final int TOTAL_ORDER_LIMIT = 5;

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
            if (bidLevel != null) { // Ensure the bid level exists
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
            vWAP = totalMarketValue / (double) totalQuantity; // Calculate VWAP
            logger.info("Current total market value = {}", totalMarketValue);
            logger.info("Current total quantity = {}", totalQuantity);
        }

        // Log the calculated VWAP
        logger.info("Current VWAP value = {}", vWAP);

        // To clarify decision-making
        logger.info("Checking if price: {} < VWAP: {}", price, vWAP);

        // Check if the total number of orders has reached or exceeded the limit
        if (totalOrders >= TOTAL_ORDER_LIMIT) {
            logger.info("[DYNAMIC-PASSIVE-ALGO] Total order limit reached. No new orders will be created.");
            logFinalState(); // Log the final state of the portfolio
            calculateROI(); // Calculate the return on investment
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

            // If none of the above conditions are met, HOLD
        } else {
            action = TradeAction.HOLD;
        }

        // Execute the above action
        switch (action) {
            case BUY:
                // Log the BUY decision
                logger.info("[DYNAMIC-PASSIVE-ALGO] Price: {} is less than VWAP: {}, buying shares", price, vWAP);
                sharesOwned += quantity; // Increase sharesOwned by the quantity bought
                totalSpent += price * quantity; // Update totalSpent by adding the cost of the purchase
                logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {}", sharesOwned, totalSpent);
                return new CreateChildOrder(Side.BUY, quantity, price); // Create a new BUY order

            case SELL:
                // Log the SELL decision
                logger.info("[DYNAMIC-PASSIVE-ALGO] Price: {} is higher than VWAP: {}, selling shares", price, vWAP);

                // Ensure there are enough shares to sell
                if (sharesOwned > 0) {
                    sharesOwned -= quantity; // Decrease sharesOwned by the quantity sold
                    totalEarned += price * quantity; // Update totalEarned by adding the revenue from the sale

                    // Calculate profit including the current market value of remaining shares
                    long marketValueOfSharesOwned = sharesOwned * price; // Current market value of owned shares
                    profit = (totalEarned + marketValueOfSharesOwned) - totalSpent; // Calculate profit

                    // Log the updated portfolio state
                    logger.info("[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {} | Total Earned: {} | Profit: {}", sharesOwned, totalSpent, totalEarned, profit);
                    return new CreateChildOrder(Side.SELL, quantity, price); // Create a new SELL order
                } else {
                    // If no shares are owned, attempt to sell what is owned (which is zero)
                    return new CreateChildOrder(Side.SELL, sharesOwned, price); // ??
                }

            case CANCEL:
                // Filter active orders to ensure they have a positive quantity
                Optional<ChildOrder> oldestValidOrderOpt = activeOrders.stream()
                        .filter(order -> order.getQuantity() > 0)
                        .findFirst();
                // Check if the oldest order exists
                if (oldestValidOrderOpt.isPresent()) {
                    ChildOrder oldestOrder = oldestValidOrderOpt.get();
                    logger.info("Current VWAP: {} (out of the acceptable range)", vWAP);
                    // Log the quantity and price of the oldest order
                    logger.info("Cancelling oldest order: Price: {}, Quantity: {}", oldestOrder.getPrice(), oldestOrder.getQuantity());

                    // Cancel the oldest order
                    return new CancelChildOrder(oldestOrder);
                } else {
                    logger.info("No valid order found to cancel.");
                    return NoAction.NoAction;
                }

            default:
                // If the action is HOLD, log the current state and take no action
                logger.info("[DYNAMIC-PASSIVE-ALGO] Holding position, no action needed. Share quantity remains: {}.", sharesOwned);
                return NoAction.NoAction;

        }

    }

    // Logs the final state of the portfolio when the order limit is reached.
    private void logFinalState() {
        logger.info("[FINAL STATE REPORT \uD83C\uDFE6] Shares Owned: {} | Total Spent: {} | Total Earned: {} | Profit: {}", sharesOwned, totalSpent, totalEarned, profit);
    }

    // Method to calculate Return on Investment (ROI)
    private void calculateROI() {
        double roi = (double) profit / totalSpent * 100; // ROI as a percentage
        logger.info("ROI \uD83D\uDCB0: {}%", roi);
    }
}