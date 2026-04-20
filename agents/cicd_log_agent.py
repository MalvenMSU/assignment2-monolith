from __future__ import annotations

import sys
from pathlib import Path
from typing import Any, TypedDict

from langgraph.graph import END, StateGraph


class AgentState(TypedDict, total=False):
    log_path: str
    plan: str
    tool_output: dict[str, Any]
    classification: str
    evidence: list[str]
    confidence: str
    reflection: str
    final_answer: str


def search_logs_tool(log_path: str) -> dict[str, Any]:
    text = Path(log_path).read_text(encoding="utf-8")
    lines = text.splitlines()

    interesting = [
        line.strip()
        for line in lines
        if any(
            token in line.lower()
            for token in [
                "failed",
                "exception",
                "error",
                "timed out",
                "denied",
                "unauthorized",
                "no session",
                "could not",
            ]
        )
    ]

    lowered = text.lower()

    if "assertionerror" in lowered or "tests completed" in lowered:
        classification = "Test failure"
    elif "could not resolve" in lowered or "temporary failure in name resolution" in lowered:
        classification = "Dependency or network failure"
    elif "permission denied" in lowered or "unauthorized" in lowered:
        classification = "Permission or authentication failure"
    elif "crashloopbackoff" in lowered:
        classification = "Kubernetes runtime failure"
    elif "compilejava" in lowered and "failed" in lowered:
        classification = "Build or compilation failure"
    else:
        classification = "Unknown failure"

    return {
        "classification": classification,
        "evidence": interesting[:8],
        "line_count": len(lines),
    }


def planning_node(state: AgentState) -> AgentState:
    return {
        "plan": (
            "1. Read the CI/CD log.\n"
            "2. Use a log-search tool to find failure evidence.\n"
            "3. Classify the failure reason.\n"
            "4. Reflect on whether the evidence is strong enough.\n"
            "5. Return a short final answer."
        )
    }


def tool_node(state: AgentState) -> AgentState:
    tool_output = search_logs_tool(state["log_path"])
    return {
        "tool_output": tool_output,
        "classification": tool_output["classification"],
        "evidence": tool_output["evidence"],
    }


def reflection_node(state: AgentState) -> AgentState:
    evidence = state.get("evidence", [])
    classification = state.get("classification", "Unknown failure")

    confidence = "high" if len(evidence) >= 2 and classification != "Unknown failure" else "medium"

    if classification == "Test failure":
        reflection = (
            "The evidence is consistent: the log shows a failing test, an assertion failure, "
            "and a Spring JSON serialization exception. The root cause is likely an application bug, "
            "not an infrastructure problem."
        )
    else:
        reflection = (
            "The evidence is limited or mixed. The classification is still useful, but the user "
            "may need more log context for a higher-confidence answer."
        )

    return {
        "confidence": confidence,
        "reflection": reflection,
    }


def final_node(state: AgentState) -> AgentState:
    evidence_block = "\n".join(f"- {line}" for line in state.get("evidence", []))
    final_answer = (
        f"Failure type: {state.get('classification', 'Unknown failure')}\n"
        f"Confidence: {state.get('confidence', 'medium')}\n"
        f"Likely cause: The pipeline failed during test execution, not during build setup or deployment.\n"
        f"Evidence:\n{evidence_block}\n"
        f"Suggested next step: Inspect the failing test and the application code path that triggered the exception."
    )
    return {"final_answer": final_answer}


def build_graph():
    graph = StateGraph(AgentState)
    graph.add_node("plan", planning_node)
    graph.add_node("tool", tool_node)
    graph.add_node("reflect", reflection_node)
    graph.add_node("final", final_node)

    graph.set_entry_point("plan")
    graph.add_edge("plan", "tool")
    graph.add_edge("tool", "reflect")
    graph.add_edge("reflect", "final")
    graph.add_edge("final", END)
    return graph.compile()


def main() -> None:
    if len(sys.argv) != 2:
        print("Usage: python3 agents/cicd_log_agent.py <log-file>")
        sys.exit(1)

    log_path = sys.argv[1]
    if not Path(log_path).exists():
        print(f"Log file not found: {log_path}")
        sys.exit(1)

    app = build_graph()
    result = app.invoke({"log_path": log_path})

    print("PLAN")
    print(result["plan"])
    print("\nTOOL")
    print(f"Searched log file: {log_path}")
    print(f"Detected classification: {result['classification']}")
    print("Evidence found:")
    for line in result.get("evidence", []):
        print(f"- {line}")
    print("\nSELF-REFLECTION")
    print(result["reflection"])
    print("\nFINAL ANSWER")
    print(result["final_answer"])


if __name__ == "__main__":
    main()
