const HANGUL_SYLLABLE_START = 0xac00;
const HANGUL_SYLLABLE_END = 0xd7a3;
const HANGUL_JUNGSEONG_COUNT = 21;
const HANGUL_JONGSEONG_COUNT = 28;
const COMPATIBILITY_CONSONANT_START = 0x3131;
const COMPATIBILITY_CONSONANT_END = 0x314e;

const INITIAL_CONSONANTS = [
  "ㄱ",
  "ㄲ",
  "ㄴ",
  "ㄷ",
  "ㄸ",
  "ㄹ",
  "ㅁ",
  "ㅂ",
  "ㅃ",
  "ㅅ",
  "ㅆ",
  "ㅇ",
  "ㅈ",
  "ㅉ",
  "ㅊ",
  "ㅋ",
  "ㅌ",
  "ㅍ",
  "ㅎ",
];

function normalizeSearchText(value: string) {
  return value.toLowerCase().replace(/\s+/g, "");
}

function isCompatibilityConsonant(char: string) {
  if (!char) {
    return false;
  }

  const code = char.charCodeAt(0);
  return code >= COMPATIBILITY_CONSONANT_START && code <= COMPATIBILITY_CONSONANT_END;
}

function getInitialConsonant(char: string) {
  if (!char) {
    return "";
  }

  const code = char.charCodeAt(0);

  if (code >= HANGUL_SYLLABLE_START && code <= HANGUL_SYLLABLE_END) {
    const syllableIndex = code - HANGUL_SYLLABLE_START;
    const initialIndex = Math.floor(
      syllableIndex / (HANGUL_JUNGSEONG_COUNT * HANGUL_JONGSEONG_COUNT),
    );

    return INITIAL_CONSONANTS[initialIndex] ?? "";
  }

  return char;
}

function matchesCharacter(queryChar: string, targetChar: string) {
  if (queryChar === targetChar) {
    return true;
  }

  if (isCompatibilityConsonant(queryChar)) {
    return getInitialConsonant(targetChar) === queryChar;
  }

  return false;
}

export function matchesHangulSearch(target: string, query: string) {
  const normalizedTarget = normalizeSearchText(target);
  const normalizedQuery = normalizeSearchText(query);

  if (!normalizedQuery) {
    return true;
  }

  if (normalizedTarget.includes(normalizedQuery)) {
    return true;
  }

  if (normalizedQuery.length > normalizedTarget.length) {
    return false;
  }

  for (
    let startIndex = 0;
    startIndex <= normalizedTarget.length - normalizedQuery.length;
    startIndex += 1
  ) {
    let matched = true;

    for (let queryIndex = 0; queryIndex < normalizedQuery.length; queryIndex += 1) {
      if (!matchesCharacter(normalizedQuery[queryIndex], normalizedTarget[startIndex + queryIndex])) {
        matched = false;
        break;
      }
    }

    if (matched) {
      return true;
    }
  }

  return false;
}
