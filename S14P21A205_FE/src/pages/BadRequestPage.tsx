import { useState } from "react";
import { useNavigate } from "react-router-dom";
import ErrorPageDetails from "../components/common/ErrorPageDetails";
import ErrorStateLayout from "../components/common/ErrorStateLayout";
import { consumeErrorPageState } from "../utils/errorPageState";

export default function BadRequestPage() {
  const navigate = useNavigate();
  const [errorState] = useState(() => consumeErrorPageState());

  return (
    <ErrorStateLayout
      code="400"
      badge="Request Invalid"
      title="That request could not be processed"
      description="The page was opened with incomplete data or a request value that no longer matches the current game flow."
      primaryAction={{
        label: "Go Back",
        onClick: () => navigate(-1),
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
