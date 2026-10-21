// 백엔드 에러 응답(ProblemDetail: { status, code, detail })을 하나의 에러 타입으로 바꾼다
// 화면은 code로 분기하고, 사용자에게는 detail(한글 메시지)을 보여 준다
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, detail: string) {
    super(detail);
    this.status = status;
    this.code = code;
  }
}

export async function readJson<T>(res: Response): Promise<T> {
  if (res.ok) {
    if (res.status === 204) return undefined as T;
    return (await res.json()) as T;
  }
  let code = 'UNKNOWN';
  let detail = '요청을 처리하지 못했습니다.';
  try {
    const body = await res.json();
    code = body.code ?? code;
    detail = body.detail ?? detail;
  } catch {
    // 본문이 JSON이 아니면(프록시 에러 등) 기본 메시지를 쓴다
  }
  throw new ApiError(res.status, code, detail);
}
