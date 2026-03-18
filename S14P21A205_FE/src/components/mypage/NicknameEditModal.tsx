import Button from "../common/Button";
import Modal from "../common/Modal";

interface NicknameEditModalProps {
  isOpen: boolean;
  nickname: string;
  error?: string;
  onChange: (value: string) => void;
  onClose: () => void;
  onSave: () => void;
}

export default function NicknameEditModal({
  isOpen,
  nickname,
  error,
  onChange,
  onClose,
  onSave,
}: NicknameEditModalProps) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title="닉네임 수정">
      <div className="space-y-5">
        <div>
          <label
            htmlFor="nickname-input"
            className="mb-2 block text-sm font-semibold text-slate-700"
          >
            새 닉네임
          </label>
          <input
            id="nickname-input"
            autoFocus
            value={nickname}
            onChange={(event) => onChange(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault();
                onSave();
              }
            }}
            className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-900 outline-none transition-colors placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
            placeholder="닉네임을 입력해 주세요"
          />
          <p className="mt-2 text-xs text-slate-400">공백만 입력하는 닉네임은 저장할 수 없어요.</p>
          {error && <p className="mt-2 text-sm text-rose-500">{error}</p>}
        </div>

        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-500 transition-colors hover:border-slate-300 hover:bg-slate-50"
          >
            취소
          </button>
          <Button onClick={onSave}>저장</Button>
        </div>
      </div>
    </Modal>
  );
}
