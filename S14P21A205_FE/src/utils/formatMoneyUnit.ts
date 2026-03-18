const formatCompactUnit = (value: number) => {
  if (value >= 100 || Number.isInteger(value)) {
    return value.toFixed(0);
  }

  return value.toFixed(1).replace(/\.0$/, "");
};

export const formatMoneyUnit = (amount: number) => {
  const absoluteAmount = Math.abs(amount);
  const prefix = amount < 0 ? "-" : "";

  if (absoluteAmount >= 10_000_000) {
    return `${prefix}${formatCompactUnit(absoluteAmount / 10_000_000)}천만원`;
  }

  if (absoluteAmount >= 1_000_000) {
    return `${prefix}${formatCompactUnit(absoluteAmount / 1_000_000)}백만원`;
  }

  return `${prefix}${formatCompactUnit(absoluteAmount / 10_000)}만원`;
};
