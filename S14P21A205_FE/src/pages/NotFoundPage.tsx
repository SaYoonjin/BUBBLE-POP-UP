import { useState } from "react";
import { useNavigate } from "react-router-dom";
import ErrorPageDetails from "../components/common/ErrorPageDetails";
import ErrorStateLayout from "../components/common/ErrorStateLayout";
import { consumeErrorPageState } from "../utils/errorPageState";

export default function NotFoundPage() {
  const navigate = useNavigate();
  const [errorState] = useState(() => consumeErrorPageState());

  return (
    <ErrorStateLayout
      code="404"
      badge="Page Missing"
      title="We could not find that page"
      description="The address may be outdated, mistyped, or no longer connected to the current season flow."
      primaryAction={{
        label: "Go Home",
        onClick: () => navigate("/", { replace: true }),
      }}
      secondaryAction={{
        label: "Go Back",
        onClick: () => navigate(-1),
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
