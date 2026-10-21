// PostToolUse(Write|Edit|MultiEdit): 핵심 로직이나 설계 문서를 바꾸면 설명 문서 동기화를 상기시킨다.
// 이 과제는 "설명 가능성"이 30%라서, 코드와 답변 문서가 어긋나는 것이 가장 큰 위험이다.
import { readFileSync } from 'node:fs';

const input = JSON.parse(readFileSync(0, 'utf8'));
const p = (input.tool_input?.file_path ?? '').replace(/\\/g, '/');

const notes = [];

if (/backend\/src\/main\/.*(order|payment|issue|install|admin|refund|status|callback|mock)/i.test(p)) {
  notes.push(
    '결제·발급·상태 관련 코드를 수정했습니다.',
    '- 관련 테스트(동시성/보상/만료)를 실행했나요? → /verify',
    '- 상태나 전이가 바뀌었다면 → /state-transition 체크리스트',
    '- 동작이나 근거가 바뀌었다면 docs/0x 설계 문서와 docs/INTERVIEW_GUIDE.md의 해당 답을 같은 커밋에서 갱신하세요.',
  );
}

if (/docs\/(0[2-9]|1[01])_/.test(p)) {
  notes.push(
    '설계 문서를 수정했습니다. docs/INTERVIEW_GUIDE.md의 30초 답변과 꼬리질문 답이 여전히 맞는지 확인하세요.',
  );
}

if (notes.length) {
  console.log(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: 'PostToolUse',
      additionalContext: '[sync-reminder]\n' + notes.join('\n'),
    },
  }));
}
process.exit(0);
