import { useState } from "react";
import { useNavigate } from "react-router-dom";
import ErrorPageDetails from "../components/common/ErrorPageDetails";
import ErrorStateLayout from "../components/common/ErrorStateLayout";
import { consumeErrorPageState } from "../utils/errorPageState";

export default function InternalServerErrorPage() {
  const navigate = useNavigate();
  const [errorState] = useState(() => consumeErrorPageState());

  return (
    <ErrorStateLayout
      code="500"
      badge="Server Error"
      title="The server could not finish that request"
      description="Something failed on our side while the current page was loading. Try again once, then return home if the issue continues."
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
