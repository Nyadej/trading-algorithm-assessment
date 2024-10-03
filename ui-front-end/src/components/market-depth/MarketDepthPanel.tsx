import React from 'react';
import { MarketDepthRow } from './MarketDepthFeature';
import './MarketDepthPanel.css';
import { PriceCell } from './PriceCell';  // Import the PriceCell component


interface MarketDepthPanelProps {
    data: MarketDepthRow[]; // a list of rows, and each row must contain data fields like level, bid, bidQuantity, etc.
}

// creating a functional component, that receives data as a prop (MarketDepthPanelProps)
export const MarketDepthPanel: React.FC<MarketDepthPanelProps> = (props) => {
const { data } = props; // destructing the props object to directly extract the data property

// JSX HTML that defines what the UI should look like
return (
        <table className="MarketDepthPanel">
        <thead>
          <tr>
            <th>Level</th>
            <th>Quantity</th>
            <th>Bid Price</th>
            <th>Ask Price</th>
            <th>Quantity</th>
          </tr>
        </thead>
        <tbody>
        {data.map((row, index) => (
         <tr key={index}>
                       <td>{row.level}</td>
                       <td>{row.bidQuantity}</td>

                       {/* Use the PriceCell for Bid Price */}
                       <PriceCell price={row.bid} />

                       {/* Use the PriceCell for Ask Price */}
                       <PriceCell price={row.offer} />

                       <td>{row.offerQuantity}</td>
                     </tr>
                 ))}
                 </tbody>
                 </table>
    );
};