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
    private long sharesOwned = 1200L; // Setting initial sharesOwned to 1200
    private long totalSpent = 60000L; // Starting with
    private long totalEarned = 0L; // track the total money earned from selling shares
    private double realisedProfit = 0; // tracks the overall profit/loss
    private double estimatedProfit = 0;

    public static final String GREEN = "\033[0;32m";     // Green text for Buy
    public static final String RED = "\033[0;31m";       // Red text for Sell
    public static final String YELLOW = "\033[0;33m";    // Yellow text for Cancel
    public static final String BLUE = "\033[0;34m";      // Blue text for Hold
    public static final String PURPLE = "\033[0;35m";    // Purple for Final Report
    public static final String RESET = "\033[0m";      // Reset text color
    public static final String CYAN = "\033[0;36m";    // Cyan text for VWAP

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

        // Get active and total orders from the current state*
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
            logger.info(CYAN + "[DYNAMIC-PASSIVE-ALGO] No active orders, using hardcoded initial VWAP: {}" + RESET, vWAP);

            // if there are active order, calculate VWAP
        } else {
            vWAP = totalMarketValue / (double) totalQuantity; // Calculate VWAP
            logger.info(CYAN + "[DYNAMIC-PASSIVE-ALGO] Current total market value = {}" + RESET, totalMarketValue);
            logger.info(CYAN + "[DYNAMIC-PASSIVE-ALGO] Current total quantity = {}" + RESET, totalQuantity);
        }

        // Log the calculated VWAP
        logger.info(CYAN + "[DYNAMIC-PASSIVE-ALGO] Current VWAP value = {}" + RESET, vWAP);

        // To clarify decision-making
        logger.info(CYAN + "[DYNAMIC-PASSIVE-ALGO] Checking if price: {} < VWAP: {}" + RESET, price, vWAP);

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
            logger.info(YELLOW + "[DYNAMIC-PASSIVE-ALGO] Cancel condition triggered: VWAP is: {} ." + RESET, vWAP);
            logger.info(YELLOW + "[DYNAMIC-PASSIVE-ALGO] Number of active orders: {}" + RESET, activeOrders.size());
            action = TradeAction.CANCEL;

            // If none of the above conditions are met, HOLD
        } else {
            action = TradeAction.HOLD;
        }

        // Execute the above action
        switch (action) {
            case BUY:
                // Log the BUY decision
                logger.info(RED + "[DYNAMIC-PASSIVE-ALGO] Price: {} is less than VWAP: {}, buying shares" + RESET, price, vWAP);
                sharesOwned += quantity; // Increase sharesOwned by the quantity bought
                totalSpent += price * quantity; // Update totalSpent by adding the cost of the purchase

                // Use VWAP to estimate the current value of shares for estimated profit calculation
                estimatedProfit = (totalEarned + sharesOwned * vWAP) - totalSpent;

                logger.info(RED + "[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {}" + RESET, sharesOwned, totalSpent);
                logger.info(RED + "[DYNAMIC-PASSIVE-ALGO] Estimated Profit (including the current market value of remaining shares): {}" + RESET, estimatedProfit);
                return new CreateChildOrder(Side.BUY, quantity, price); // Create a new BUY order

            case SELL:
                // Log the SELL decision
                logger.info(GREEN + "[DYNAMIC-PASSIVE-ALGO] Price: {} is higher than VWAP: {}, selling shares" + RESET, price, vWAP);

                // Ensure there are enough shares to sell
                if (sharesOwned > 0) {
                    sharesOwned -= quantity; // Decrease sharesOwned by the quantity sold
                    totalEarned += price * quantity; // Update totalEarned by adding the revenue from the sale

                    realisedProfit = totalEarned - totalSpent; // Calculate profit
                    // Calculate estimated profit, which includes the value of remaining shares at the current market price
                    estimatedProfit = (totalEarned + sharesOwned * price) - totalSpent;

                    // Log the updated portfolio state
                    logger.info(GREEN + "[DYNAMIC-PASSIVE-ALGO] Current Shares: {} | Total Spent: {} | Total Earned: {} | Profit: {}" + RESET, sharesOwned, totalSpent, totalEarned, realisedProfit);
                    logger.info(GREEN + "[DYNAMIC-PASSIVE-ALGO] Estimated Profit (including the current market value of remaining shares): {}" + RESET, estimatedProfit);
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
                    logger.info(YELLOW + "[DYNAMIC-PASSIVE-ALGO] Current VWAP: {} (out of the acceptable range)" + RESET, vWAP);
                    // Log the quantity and price of the oldest order
                    logger.info(YELLOW + "[DYNAMIC-PASSIVE-ALGO] Cancelling oldest order: Price: {}, Quantity: {}" + RESET, oldestOrder.getPrice(), oldestOrder.getQuantity());

                    // Cancel the oldest order
                    return new CancelChildOrder(oldestOrder);
                } else {
                    logger.info(YELLOW + "No valid order found to cancel." + RESET);
                    return NoAction.NoAction;
                }

            default:
                // If the action is HOLD, log the current state and take no action
                logger.info(BLUE + "[DYNAMIC-PASSIVE-ALGO] Holding position, no action needed. Share quantity remains: {}. Trying to match and fill orders, otherwise will remove." + RESET, sharesOwned);
                return NoAction.NoAction;
        }
    }

    // Logs the final state of the portfolio when the order limit is reached.
    private void logFinalState() {
        logger.info(PURPLE + "[FINAL STATE REPORT \uD83C\uDFE6] Shares Owned: {} | Total Spent: {} | Total Earned: {} | Estimated Profit: {}" + RESET, sharesOwned, totalSpent, totalEarned, estimatedProfit);
    }

    // Method to calculate Return on Investment (ROI)
    private void calculateROI() {
        double roi = realisedProfit / totalSpent * 100; // ROI as a percentage
        double estimatedRoi = estimatedProfit / totalSpent * 100;
        logger.info(PURPLE + "ROI \uD83D\uDCB0: {}% | Estimated ROI \uD83D\uDCC8: {}%" + RESET, roi, estimatedRoi);
    }
}