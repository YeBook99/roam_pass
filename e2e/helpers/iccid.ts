// 테스트용 ICCID 생성: 89(통신) + 국가/사업자 + 일련번호 + Luhn 체크 디지트 = 19자리

export function luhnCheckDigit(body: string): number {
  let sum = 0;
  for (let i = 0; i < body.length; i++) {
    let d = Number(body[body.length - 1 - i]);
    if (i % 2 === 0) {                 // 체크 디지트를 붙였을 때 오른쪽에서 두 번째 자리부터 두 배
      d *= 2;
      if (d > 9) d -= 9;
    }
    sum += d;
  }
  return (10 - (sum % 10)) % 10;
}

export function validIccid(): string {
  let body = '8982';
  while (body.length < 18) body += Math.floor(Math.random() * 10);
  return body + luhnCheckDigit(body);
}

/** 자릿수는 맞지만 체크 디지트가 틀린 ICCID */
export function invalidChecksumIccid(): string {
  const ok = validIccid();
  const wrong = (Number(ok.at(-1)) + 1) % 10;
  return ok.slice(0, -1) + wrong;
}
