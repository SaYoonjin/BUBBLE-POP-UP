import { useState } from "react";
import { useNavigate } from "react-router-dom";
import ErrorPageDetails from "../components/common/ErrorPageDetails";
import ErrorStateLayout from "../components/common/ErrorStateLayout";
import { consumeErrorPageState } from "../utils/errorPageState";

export default function UnauthorizedPage() {
  const navigate = useNavigate();
  const [errorState] = useState(() => consumeErrorPageState());

  return (
    <ErrorStateLayout
      code="401"
      badge="Session Expired"
      title="Authentication is required again"
      description="Your session is no longer valid. Sign in again to continue your current season flow safely."
      primaryAction={{
        label: "Go To Login",
        onClick: () => navigate("/login", { replace: true }),
      }}
      secondaryAction={{
        label: "Go Home",
        onClick: () => navigate("/", { replace: true }),
        variant: "secondary",
      }}
      footer={(
        <ErrorPageDetails
          code={errorState?.code}
          message={errorState?.message}
          path={errorState?.path}
        />
      )}
    />
  );
}
