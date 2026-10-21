// 상태 표시의 색 종류. 색은 의미가 있을 때만 쓴다 (디자인 기준: 틀은 무채색, 색은 상태와 데이터에만)
// positive = 끝났고 정상, negative = 사람이 봐야 하는 실패, warning = 아직 진행 중, neutral = 그 밖
// 어떤 상태가 어떤 색인지는 각 기능의 types.ts가 Record<상태, Tone>으로 정한다 → 상태가 빠지면 빌드가 실패한다
export type Tone = 'positive' | 'negative' | 'warning' | 'neutral';
