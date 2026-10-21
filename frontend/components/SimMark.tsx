// 서비스 표시: 모서리 하나가 잘린 SIM 카드 모양과 그 안의 칩. 장식이 아니라 "무엇을 파는 곳인지"를 알려 준다
export function SimMark() {
  return (
    <svg width="18" height="22" viewBox="0 0 18 22" aria-hidden="true">
      <path d="M1 1h11l5 5v15H1z" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
      <rect x="5" y="9" width="8" height="7" rx="1" fill="none" stroke="currentColor" strokeWidth="1.5" />
    </svg>
  );
}
