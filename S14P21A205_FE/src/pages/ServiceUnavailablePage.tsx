import { useState } from "react";
import { useNavigate } from "react-router-dom";
import ErrorPageDetails from "../components/common/ErrorPageDetails";
import ErrorStateLayout from "../components/common/ErrorStateLayout";
import { consumeErrorPageState } from "../utils/errorPageState";

export default function ServiceUnavailablePage() {
  const navigate = useNavigate();
  const [errorState] = useState(() => consumeErrorPageState());

  return (
    <ErrorStateLayout
      code="503"
      badge="Temporarily Unavailable"
      title="The service is taking a short pause"
      description="The backend is in maintenance mode or temporarily unavailable. Wait a moment and retry when the service stabilizes."
      primaryAction={{
        label: "Try Again",
        onClick: () => window.location.reload(),
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
