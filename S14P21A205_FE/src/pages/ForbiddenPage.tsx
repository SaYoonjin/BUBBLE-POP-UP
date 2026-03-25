import { useNavigate } from "react-router-dom";
import ErrorStateLayout from "../components/common/ErrorStateLayout";

export default function ForbiddenPage() {
  const navigate = useNavigate();

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
    />
  );
}
