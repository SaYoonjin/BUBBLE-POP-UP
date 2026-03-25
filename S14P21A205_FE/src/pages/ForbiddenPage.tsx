import { useState } from "react";
import { useNavigate } from "react-router-dom";
import ErrorPageDetails from "../components/common/ErrorPageDetails";
import ErrorStateLayout from "../components/common/ErrorStateLayout";
import { consumeErrorPageState } from "../utils/errorPageState";

export default function ForbiddenPage() {
  const navigate = useNavigate();
  const [errorState] = useState(() => consumeErrorPageState());

  return (
    <ErrorStateLayout
      code="403"
      badge="Access Restricted"
      title="This page is off limits"
      description="Your account can sign in, but this page is not available from your current flow or permissions."
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
