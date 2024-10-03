import { useRef } from "react";

interface PriceCellProps {
  price: number;
}

export const PriceCell = (props: PriceCellProps) => {
  const { price } = props;

  // Store the previous price using useRef
  const lastPriceRef = useRef(price);

  // Calculate the difference between the current and last price
  const diff = price - lastPriceRef.current;

  // Update the last price to the current one for the next render
  lastPriceRef.current = price;

  // Determine the arrow based on the price difference
  const arrow = diff > 0 ? "↑" : diff < 0 ? "↓" : "";

  return (
    <td>
      {price} <span className={arrow === "↑" ? "up-arrow" : "down-arrow"}>{arrow}</span>
    </td>
  );
};