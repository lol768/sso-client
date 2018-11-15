package uk.ac.warwick.userlookup;

public abstract class AbstractUserAttributesAdapter implements UserAttributesAdapter {
  abstract protected String get(String name);

  @Override
  public String getUserType() {
    return get("urn:websignon:usertype");
  }

  @Override
  public boolean isStaff() {
    return "true".equalsIgnoreCase(get("staff"));
  }

  @Override
  public boolean isStudent() {
    return "true".equalsIgnoreCase(get("student"));
  }

  @Override
  public boolean isAlumni() {
    return "true".equalsIgnoreCase(get("alumni")) || "Alumni".equalsIgnoreCase(getUserType());
  }

  @Override
  public boolean isApplicant() {
    return "Applicant".equalsIgnoreCase(getUserType());
  }

  @Override
  public boolean isLoginDisabled() {
    return "true".equalsIgnoreCase(get("logindisabled"));
  }

  @Override
  public boolean isWarwickPrimary() {
    return "yes".equalsIgnoreCase(get("warwickprimary"));
  }

  @Override
  public String getUserSource() {
    return get("urn:websignon:usersource");
  }
}
