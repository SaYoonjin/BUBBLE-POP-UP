import { useNavigate } from "react-router-dom";
import ErrorStateLayout from "../components/common/ErrorStateLayout";

export default function NotFoundPage() {
  const navigate = useNavigate();

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
    />
  );
}
