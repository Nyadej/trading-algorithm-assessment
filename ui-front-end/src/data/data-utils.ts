import { pricesDataRow } from "./pricesTable";

export type PriceGenerator = (min: number, max: number) => number;

export function random(min: number, max: number) {
  min = Math.ceil(min);
  max = Math.floor(max);
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

export const nextRandomDouble: PriceGenerator = (min, max) =>
  min + (max - min) * Math.random();

export function randomPercentage(value: number) {
  const dec = random(1, 100);
  const percentage = dec / 100;
  return value * percentage;
}

const array = [];
for (let i = 0; i < 40; i++) {
  array.push(random(1, 100));
}
console.log(array.sort((a, b) => a - b).join(","));

export const asPrice = (n: number, digits = 2) =>
  parseFloat((n * 1.0).toFixed(digits));

export const randomQuantityChange = (seed: number) => {
  return Math.round(seed + seed * (1 / random(5, 20)));
};

export const generateMarketDepth = (symbolId: string) => {
  const seedPrice = 50;
  let seedBidQuantity = 1500;
  let seedOfferQuantity = 1000;
  let levelBid = seedPrice;
  let levelOffer = seedPrice;
  let priceVariance = 1;

  const pricesData: pricesDataRow[] = [];

  for (let i = 0; i < 10; i++) {
    const symbolLevel = `${symbolId}-${i}`;

    const bid = asPrice(levelBid - 1 * randomPercentage(priceVariance), 2);
    const bidQuantity = seedBidQuantity + randomQuantityChange(seedBidQuantity);

    const offer = asPrice(levelOffer + 1 * randomPercentage(priceVariance), 2);
    const offerQuantity =
      seedOfferQuantity + randomQuantityChange(seedOfferQuantity);

    pricesData.push([
      bid,
      bidQuantity,
      i,
      offer,
      offerQuantity,
      symbolId,
      symbolLevel,
    ]);

    levelBid = bid;
    levelOffer = offer;
    priceVariance += 0.2;
    seedBidQuantity += random(20, 80);
    seedOfferQuantity += random(20, 80);
  }
  return pricesData;
};
